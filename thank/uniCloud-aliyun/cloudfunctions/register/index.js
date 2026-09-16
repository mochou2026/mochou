// uniCloud-aliyun/cloudfunctions/register/index.js
'use strict';
const crypto = require('crypto'); // 用于密码加密
const db = uniCloud.database();
const userCollection = db.collection('user');

exports.main = async (event, context) => {
  const { username, password, deviceId } = event;
  
  // 1. 参数校验
  if (!username || !password || !deviceId) {
    return { code: 400, msg: '参数不全' };
  }
  if (username.length < 2 || password.length < 6) {
    return { code: 400, msg: '用户名至少2位，密码至少6位' };
  }
  
  // 2. 检查该设备是否已注册
  const deviceCheck = await userCollection.where({ deviceId }).get();
  if (deviceCheck.data.length > 0) {
    return { code: 400, msg: '该设备已注册过账号' };
  }
  
  // 3. 检查用户名是否已存在
  const userCheck = await userCollection.where({ username }).get();
  if (userCheck.data.length > 0) {
    return { code: 400, msg: '用户名已存在' };
  }
  
  // 4. 密码加密（sha256 + 盐）
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.pbkdf2Sync(password, salt, 1000, 64, 'sha256').toString('hex');
  const encryptedPassword = salt + ':' + hash;
  
  // 5. 写入数据库
  const record = {
    username,
    password: encryptedPassword,
    deviceId,
    createTime: Date.now()
  };
  await userCollection.add(record);
  
  return { code: 200, msg: '注册成功' };
};