/*
 * @Description: node服务
 * @Version: 4.0.0
 * @Date: 2021-07-06 11:39:38
 * @LastEditors: Yawen Yang
 * @LastEditTime: 2023-09-08 15:58:59
 */
const koa = require('koa');
const axios = require('axios');
 // 引入 koa-static
const staticFiles = require('koa-static')

const Router = require('koa-router')

const crypto = require('crypto');
const app = new koa();
const router = new Router()
app.use(staticFiles('./'))

const keyMap = {
    'd4b6324f31f5584958fefb4081c1a3bf': '8d8846f4f1033bf6529e6e456981afea',
    'e6d18f2ae1d1d3459ada191a13045134': 'f4443aa37e138d1002b45ce99e8ff89e',
    'b191a938ad777ca669fb05f38fae1c47': '514efcc58b1db869afa7e96ea0153e5e',
    '389a0d3be647360d9ab264d8d39f046a': '8becfdf50721a42ed4e89f8cbfa59dfc'
};

app.use(async(ctx, next)=>{
    ctx.set('Access-Control-Allow-Origin', '*');
    ctx.set('Access-Control-Allow-Headers', 'Content-Type, Content-Length, Authorization, Accept, X-Requested-With');
    ctx.set('Access-Control-Allow-Methods', 'PUT, POST, GET, DELETE, OPTIONS');
    if (ctx.method == 'OPTIONS') {
      ctx.body = 200; 
    } else {
     await next();
    }
})

router.get('/demo/login', async(ctx, next) => {
    const params = ctx.query;
    const prikey = keyMap[params.captcha_id];

    if(!prikey){
        ctx.response.status = 200;
        ctx.response.body = {'result': 'fail', 'reason': 'id is not in id pools '};;
    }

    const sign_token = crypto.createHmac('sha256', prikey).update(params.lot_number).digest('hex');
    const query = Object.assign(params,{
        sign_token
    });
    const result = await axios({
        method: 'get',
        params: query,
        url: 'http://gcaptcha4.geetest.com/validate'
    }).then((res)=>{
        return res.data
    }).catch(()=>{
        return {'result': 'success', 'reason': 'request geetest api fail'};
    })
    ctx.response.status = 200;
    ctx.response.body = result;
})

app.use(router.routes())
    .listen(9013, ()=>{
        console.log('server is running in 9013');
    });