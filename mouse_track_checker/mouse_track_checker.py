import json
import traceback
from flask import Flask, jsonify, Response
from flask import request
from flask_cors import CORS
import numpy as np
from redis_connection import r

app = Flask(__name__)
CORS(app, supports_credentials=True)

THRESHOLD_SLOW_DIFF_TIME = 10
THRESHOLD_SLOW_RATE = 0.5

SCREEN_WIDTH = 3840
SCREEN_HEIGHT = 2160
BIN_SPLIT_NX = 4
BIN_SPLIT_NY = 4

THRESHOLD_SIMILAR = 0.001


@app.route('/check_mouse_track', methods=['POST'])
def check_mouse_track() -> Response:
    try:
        body = request.get_json()
        mouse_track = np.array(body['mouseTrack'])
        print('mouse_track:', mouse_track)
        ip = body['ip']
        print('ip:', ip)
        frame_flag = check_low_frame_rate(mouse_track)
        print('low_frame_rate:', frame_flag)
        vec = to_vec(mouse_track)
        history = get_history(ip)
        similar_flag = False
        if len(history) > 0:
            similar_flag = check_similar(vec, history)
        print('similar_flag:', similar_flag)
        res = not frame_flag and not similar_flag
        if res:
            save_history(ip, vec)
        return jsonify(res)
    except:
        traceback.print_exc()
        return jsonify(False)


def check_low_frame_rate(mouse_track: np.ndarray) -> bool:
    diff_time = mouse_track[1:, -1] - mouse_track[:-1, -1]
    num_slow = np.sum(diff_time > THRESHOLD_SLOW_DIFF_TIME)
    rate = num_slow / len(diff_time)
    print('slow_rate:', rate)
    return rate > THRESHOLD_SLOW_RATE


def to_vec(mouse_track: np.ndarray) -> np.ndarray:
    hist_x, _ = np.histogram(mouse_track[:, 0], bins=SCREEN_WIDTH // BIN_SPLIT_NX, range=(0, SCREEN_WIDTH),
                             density=True)
    hist_y, _ = np.histogram(mouse_track[:, 1], bins=SCREEN_HEIGHT // BIN_SPLIT_NY, range=(0, SCREEN_HEIGHT),
                             density=True)
    return np.r_[hist_x, hist_y]


def get_history(ip: str) -> np.ndarray:
    key = 'history:{}'.format(ip)
    data = r.lrange(key, 0, -1)
    for i in range(len(data)):
        data[i] = json.loads(data[i])
    return np.array(data)


def save_history(ip: str, vec: np.ndarray):
    key = 'history:{}'.format(ip)
    r.rpush(key, json.dumps(vec.tolist()))
    r.expire(key, 60 * 60 * 24)


def check_similar(vec: np.ndarray, others: np.ndarray) -> bool:
    dis_sqr = np.sum((others - vec) ** 2, axis=1)
    min_dis_sqr = np.min(dis_sqr)
    print('difference_to_history:', min_dis_sqr)
    return min_dis_sqr < THRESHOLD_SIMILAR
