<template>
    <div class='jc-component__range'>
        <div class='jc-range' :class="rangeStatus?'success':''">
            <i @mousedown='rangeMove' :class='rangeStatus?successIcon:startIcon'></i>
            {{ rangeStatus ? successText : startText }}
        </div>
    </div>
</template>
<script>
import Bus from './bus';
import Aips from '../../api/account';

export default {
    props: {
        // 成功之后的函数
        successFun: {
            type: Function
        },
        //成功图标
        successIcon: {
            type: String,
            default: 'el-icon-success'
        },
        //成功文字
        successText: {
            type: String,
            default: '验证成功'
        },
        //开始的图标
        startIcon: {
            type: String,
            default: 'el-icon-d-arrow-right'
        },
        //开始的文字
        startText: {
            type: String,
            default: '拖动滑块到最右边'
        },
        //失败之后的函数
        errorFun: {
            type: Function
        },
        //或者用值来进行监听
        status: {
            type: String
        }
    },
    name: 'Silder',
    data() {
        return {
            rangeStatus: ''
        };
    },
    methods: {
        rangeMove(e) {
            let ele = e.target;
            let startX = e.clientX;
            let eleWidth = ele.offsetWidth;
            let parentWidth = ele.parentElement.offsetWidth;
            let MaxX = parentWidth - eleWidth;
            let mouseTrack = [];
            let count = 0;
            let time_last = 0;
            if (this.rangeStatus) {//不运行
                return false;
            }
            // 获取鼠标轨迹
            document.onpointermove = function(event) {
                let pointerEvents = event.getCoalescedEvents();
                for (let i = 0; i < pointerEvents.length; i++) {
                    count++;
                    mouseTrack.push([pointerEvents[i].pageX, pointerEvents[i].pageY, pointerEvents[i].timeStamp]);
                    console.log(count, pointerEvents[i].pageX, pointerEvents[i].pageY,
                        pointerEvents[i].timeStamp, pointerEvents[i].timeStamp - time_last);
                    time_last = pointerEvents[i].timeStamp;
                }
            };
            document.onmousemove = (e) => {
                let endX = e.clientX;
                this.disX = endX - startX;
                if (this.disX <= 0) {
                    this.disX = 0;
                }
                if (this.disX >= MaxX - eleWidth) {//减去滑块的宽度,体验效果更好
                    this.disX = MaxX;
                }
                ele.style.transition = '.1s all';
                ele.style.transform = 'translateX(' + this.disX + 'px)';
                e.preventDefault();
            };
            document.onmouseup = () => {
                if (this.disX !== MaxX) {
                    // 滑块未完成
                    ele.style.transition = '.5s all';
                    ele.style.transform = 'translateX(0)';
                    //执行失败的函数
                    this.errorFun && this.errorFun();
                } else {
                    // 滑块完成后验证轨迹
                    Aips.checkMouseTrack(mouseTrack).then(res => {
                        console.log(res);
                        if (res.code == 0 && res.data == true) {
                            this.rangeStatus = true;
                            // if(this.status){
                            // 	this.$parent[this.status] = true;
                            // }
                            //执行成功的函数
                            Bus.$emit('sliderChange', true);
                            this.successFun && this.successFun();
                        }
            			// 轨迹不正确
            			else{
            				ele.style.transition = '.5s all';
            				ele.style.transform = 'translateX(0)';
            				//执行失败的函数
            				this.$message.error("请重试")
            				this.errorFun && this.errorFun();
            			}
                    }, err => {
                        ele.style.transition = '.5s all';
                        ele.style.transform = 'translateX(0)';
                        //执行失败的函数
                        this.$message.error("请重试")
                        this.errorFun && this.errorFun();
                    }).catch(err => {
                        ele.style.transition = '.5s all';
                        ele.style.transform = 'translateX(0)';
                        //执行失败的函数
                        this.$message.error("请重试")
                        this.errorFun && this.errorFun();
                    });
                }
                // 清空轨迹
                mouseTrack = [];
                // 清除滑动指针事件
                document.onmousemove = null;
                document.onmouseup = null;
                document.onpointermove = null;
            };
        }
    }
};
</script>
<style scoped>
.jc-range {
    background-color: #FFCCCC;
    position: relative;
    transition: 1s all;
    user-select: none;
    color: #333;
    display: flex;
    justify-content: center;
    align-items: center;
    height: 45px; /*no*/
}

.jc-range i {
    position: absolute;
    left: 0;
    width: 60px; /*no*/
    height: 100%;
    color: #919191;
    background-color: #fff;
    border: 1px solid #bbb;
    cursor: pointer;
    display: flex;
    justify-content: center;
    align-items: center;
}

.jc-range.success {
    background-color: #7AC23C;
    color: #fff;
}

.jc-range.success i {
    color: #7AC23C;
}
</style>
