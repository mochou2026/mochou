// uniCloud-aliyun/cloudfunctions/resetPassword/index.js
'use strict';
const crypto = require('crypto');
const db = uniCloud.database();
const userCollection = db.collection('user');

exports.main = async (event, context) => {
  const { username, oldPassword, newPassword } = event;
  if (!username || !oldPassword || !newPassword) {
    return { code: 400, msg: '参数不全' };
  }
  if (newPassword.length < 6) {
    return { code: 400, msg: '新密码至少6位' };
  }
  
  // 1. 查找用户
  const res = await userCollection.where({ username }).get();
  if (res.data.length === 0) {
    return { code: 400, msg: '用户不存在' };
  }
  const user = res.data[0];
  
  // 2. 验证旧密码
  const [salt, storedHash] = user.password.split(':');
  const oldHash = crypto.pbkdf2Sync(oldPassword, salt, 1000, 64, 'sha256').toString('hex');
  if (oldHash !== storedHash) {
    return { code: 400, msg: '旧密码错误' };
  }
  
  // 3. 对新密码加密
  const newSalt = crypto.randomBytes(16).toString('hex');
  const newHash = crypto.pbkdf2Sync(newPassword, newSalt, 1000, 64, 'sha256').toString('hex');
  const newEncrypted = newSalt + ':' + newHash;
  
  // 4. 更新密码
  await userCollection.doc(user._id).update({
    password: newEncrypted
  });
  
  return { code: 200, msg: '密码修改成功' };
};