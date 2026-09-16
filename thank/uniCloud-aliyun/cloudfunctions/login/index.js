// uniCloud-aliyun/cloudfunctions/login/index.js
'use strict';
const crypto = require('crypto');
const db = uniCloud.database();
const userCollection = db.collection('user');

exports.main = async (event, context) => {
  const { username, password } = event;
  if (!username || !password) {
    return { code: 400, msg: '用户名和密码不能为空' };
  }
  
  // 1. 查找用户
  const res = await userCollection.where({ username }).get();
  if (res.data.length === 0) {
    return { code: 400, msg: '用户不存在' };
  }
  const user = res.data[0];
  
  // 2. 验证密码
  const [salt, storedHash] = user.password.split(':');
  const hash = crypto.pbkdf2Sync(password, salt, 1000, 64, 'sha256').toString('hex');
  
  if (hash === storedHash) {
    // 登录成功，返回用户信息（不包含密码）
    delete user.password;
    return { code: 200, msg: '登录成功', data: user };
  } else {
    return { code: 400, msg: '密码错误' };
  }
};