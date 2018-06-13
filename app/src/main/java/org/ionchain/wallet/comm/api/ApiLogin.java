package org.ionchain.wallet.comm.api;

import org.ionchain.wallet.comm.api.conf.ApiConfig;
import org.ionchain.wallet.comm.api.constant.ApiConstant;
import org.ionchain.wallet.comm.api.request.ViewParm;
import org.ionchain.wallet.comm.constants.Comm;
import org.ionchain.wallet.ui.comm.BaseActivity;

import java.lang.reflect.Type;
import java.util.HashMap;

public class ApiLogin extends BaseApi {

    /**
     * 登录
     *
     * @param tel
     * @param password
     * @param viewParm
     */
    public static void login(String tel, String password, ViewParm viewParm) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("tel", tel);
        map.put("password", password);
        String url = ApiConfig.API_BASE_URL + ApiConstant.ApiUri.URI_LOGIN.getDesc();
        postJson(url, map, viewParm);

    }

    /**
     * 获取短信验证码
     *
     * @param tel
     * @param smsCode
     * @param password
     * @param inviteCode
     * @param viewParm
     */
    public void sendSmsCode(String tel, String smsCode, String password, String inviteCode, ViewParm viewParm) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("tel", tel);
        String url = ApiConfig.API_BASE_URL + ApiConstant.ApiUri.URI_SMS_CODE.getDesc();
        postJson(url, map, viewParm);
    }

    /**
     * 注册
     *
     * @param tel
     * @param smsCode
     * @param password
     * @param inviteCode
     * @param viewParm
     */
    public void register(String tel, String smsCode, String password, String inviteCode, ViewParm viewParm) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("tel", tel);
        map.put("smsCode", smsCode);
        map.put("password", password);
        map.put("inviteCode", inviteCode);
        String url = ApiConfig.API_BASE_URL + ApiConstant.ApiUri.URI_REG.getDesc();
        postJson(url, map, viewParm);
    }

    /**
     * 修改密码
     * "smsCode":"5400",
     * "tel":"18621870243",
     * "newpassword":"123456AaB"
     */
    public void updatePassWord(String tel, String smsCode, String password, ViewParm viewParm) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("tel", tel);
        map.put("smsCode", smsCode);
        map.put("password", password);
        String url = ApiConfig.API_BASE_URL + ApiConstant.ApiUri.URI_EDIT_PASS.getDesc();
        postJson(url, map, viewParm);
    }


}
