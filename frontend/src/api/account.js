import request from '../utils/request';
var url="http://127.0.0.1:9100"
export default{
    register: data=>{
        return request({
            url: url+'/auth/register',
            headers:{
                'ip':data.environment.ip,
                'deviceId':data.environment.deviceId
            },
            method: "post",
            data:data
        })

    },
    loginWithAccount: data=>{
        return request({
            url: url+'/auth/loginWithUsername',
            headers:{
                'ip':data.environment.ip,
                'deviceId':data.environment.deviceId
            },
            method:"post",
            data:data
        })
    },
    loginWithPhone: data=>{
        return request({
            url: url+'/auth/loginWithPhone',
            headers:{
                'ip':data.environment.ip,
                'deviceId':data.environment.deviceId
            },
            method:"post",
            data:data
        })
    },
    applyCode: data=>{
        return request({
            url: url+'/verifyCode/applyCode',
            headers:{
                'ip':data.environment.ip,
                'deviceId':data.environment.deviceId
            },
            method:"post",
            data:data
        })
    },
    getUser: ()=>{
        return request({
            url: url+'/auth/getUser',
            headers:{
				// 'ip':data.environment.ip,
				// 'deviceId':data.environment.deviceId,
				'token':localStorage.getItem('sessionId'),
            },
            method:"post",
        })
    },
    getLoginRecord: ()=>{
        return request({
            url: url+'/auth/getLoginRecord',
            headers:{
                // 'ip':data.environment.ip,
                // 'deviceId':data.environment.deviceId,
				'token':localStorage.getItem('sessionId'),
            },
            method:"post",
            // data:data,
        })
    },
    logout: data=>{
        return request({
            url: url+'/auth/logout',
            headers:{
                // 'ip':data.environment.ip,
                // 'deviceId':data.environment.deviceId,
				'token':localStorage.getItem('sessionId'),
            },
            method:"post",
            data:data
        })
    },
	checkToken: ()=>{
		return request({
			url: url+'/auth/checkToken',
			headers:{
				// 'ip':'127.0.0.1',
				// 'deviceId':data.environment.deviceId,
				'token':localStorage.getItem('sessionId'),
			},
			method:"get",
		})
	},
	getPublicKey: ()=>{
		return request({
			url: url+'/auth/getPublicKey',
			headers:{
				// 'ip':'127.0.0.1',
				// 'deviceId':data.environment.deviceId,
				// 'token':localStorage.getItem('sessionId'),
			},
			method:"get",
		})
	},
	checkMouseTrack:(data)=>{
		return request({
			url: url+'/auth/checkMouseTrack',
			headers:{
				// 'ip':'127.0.0.1',
				// 'deviceId':data.environment.deviceId,
				'token':localStorage.getItem('sessionId'),
			},
			data:data||[],
			method:"post",
		})
	}
}
