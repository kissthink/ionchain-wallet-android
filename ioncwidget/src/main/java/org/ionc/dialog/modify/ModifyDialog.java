package org.ionc.dialog.modify;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.ionc.dialog.R;

import org.ionc.LogUtils;
import org.ionc.dialog.base.AbsBaseDialog;

/**
 * describe: 修改密码
 *
 * @author xubinbin
 * @date 2019/03/12
 */
public class ModifyDialog extends AbsBaseDialog implements View.OnClickListener {
    private AppCompatEditText modifyDialogCurrentPasswordEt;
    private AppCompatEditText newPasswordEt;
    private ImageView showNewPasswordImg;
    private AppCompatEditText newPasswordAgainEt;
    private ImageView showNewPasswordAgainImg;
    private AppCompatButton modifyBtnCancel;
    private AppCompatButton modifyBtnSure;

    private String currentPassword;//当前密码
    private String newPassword;//新密码
    private String newPasswordAgain;//重复新密码


    private boolean newPasswordIsShow = false;
    private boolean newPasswordAgainIsShow = false;

    private OnModifyPasswordCallback modifyPasswordCallback;


    /**
     * Find the Views in the layout<br />
     * <br />
     * Auto-created on 2019-03-12 13:40:56 by Android Layout Finder
     * (http://www.buzzingandroid.com/tools/android-layout-finder)
     */
    private void findViews() {
        //输入框
        modifyDialogCurrentPasswordEt = (AppCompatEditText) findViewById(R.id.modify_dialog_current_password_et);
        newPasswordEt = (AppCompatEditText) findViewById(R.id.modify_dialog_new_password_et);
        newPasswordAgainEt = (AppCompatEditText) findViewById(R.id.modify_dialog_new_password_again_et);

        //小眼睛
        showNewPasswordImg = (ImageView) findViewById(R.id.show_new_password);
        showNewPasswordAgainImg = (ImageView) findViewById(R.id.show_new_password_again);
        showNewPasswordImg.setOnClickListener(this);
        showNewPasswordAgainImg.setOnClickListener(this);
        //按钮
        modifyBtnCancel = (AppCompatButton) findViewById(R.id.modify_btn_cancel);
        modifyBtnSure = (AppCompatButton) findViewById(R.id.modify_btn_sure);
        modifyBtnCancel.setOnClickListener(this);
        modifyBtnSure.setOnClickListener(this);
    }

    /**
     * Handle button click events<br />
     * <br />
     * Auto-created on 2019-03-12 13:40:56 by Android Layout Finder
     * (http://www.buzzingandroid.com/tools/android-layout-finder)
     */
    @Override
    public void onClick(View v) {
        if (v == showNewPasswordImg) {
            //新密码小眼睛
            if (newPasswordIsShow) {
                newPasswordIsShow = false;
                newPasswordEt.setTransformationMethod(PasswordTransformationMethod.getInstance());
                showNewPasswordImg.setImageResource(R.mipmap.hide_password);
            } else {
                newPasswordIsShow = true;
                showNewPasswordImg.setImageResource(R.mipmap.show_password);
                newPasswordEt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
            if (!TextUtils.isEmpty(newPassword)) {
                newPasswordEt.setSelection(newPassword.length());
            }

        } else if (v == showNewPasswordAgainImg) {
            //重复新密码小眼睛
            if (newPasswordAgainIsShow) {
                newPasswordAgainIsShow = false;
                newPasswordAgainEt.setTransformationMethod(PasswordTransformationMethod.getInstance());
                showNewPasswordAgainImg.setImageResource(R.mipmap.hide_password);
            } else {
                newPasswordAgainIsShow = true;
                newPasswordAgainEt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                showNewPasswordAgainImg.setImageResource(R.mipmap.show_password);
            }
            if (!TextUtils.isEmpty(newPasswordAgain)) {
                newPasswordAgainEt.setSelection(newPasswordAgain.length());
            }
        } else if (v == modifyBtnCancel) {
            dismiss();
        } else if (v == modifyBtnSure) {
            //回调给宿主
            if (modifyDialogCurrentPasswordEt.getText() != null) {
                currentPassword = modifyDialogCurrentPasswordEt.getText().toString();
                if (TextUtils.isEmpty(currentPassword)) {
                    Toast.makeText(mActivity, "请输入当前密码", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (newPasswordEt.getText() != null) {
                newPassword = newPasswordEt.getText().toString();
                if (TextUtils.isEmpty(newPassword)) {
                    Toast.makeText(mActivity, "请输入新密码", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (newPasswordAgainEt.getText() != null) {
                newPasswordAgain = newPasswordAgainEt.getText().toString();
                if (TextUtils.isEmpty(newPasswordAgain)) {
                    Toast.makeText(mActivity, "请再次输入新密码", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            modifyPasswordCallback.onModifyDialogParam(currentPassword, newPassword, newPasswordAgain);
        }
    }

    public ModifyDialog(@NonNull Context context, OnModifyPasswordCallback callback) {
        super(context);
        modifyPasswordCallback = callback;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initDialog() {
        /*
         * 获取圣诞框的窗口对象及参数对象以修改对话框的布局设置,
         * 可以直接调用getWindow(),表示获得这个Activity的Window
         * 对象,这样这可以以同样的方式改变这个Activity的属性.
         */
        Window dialogWindow = getWindow();

        /*
         * 将对话框的大小按屏幕大小的百分比设置
         */
        WindowManager m = mActivity.getWindowManager();
        Display d = m.getDefaultDisplay(); // 获取屏幕宽、高用
        WindowManager.LayoutParams p; // 获取对话框当前的参数值
        if (dialogWindow != null) {
            p = dialogWindow.getAttributes();
//            p.height = (int) (d.getHeight() * 0.6); // 高度设置为屏幕的0.6
            p.width = (int) (d.getWidth() * 0.9); // 宽度设置为屏幕的0.65
            dialogWindow.setAttributes(p);
        } else {
            LogUtils.e("设置对话框带下大小失败");
        }
    }

    @Override
    protected void initView() {
        findViews();
        modifyDialogCurrentPasswordEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        //新密码
        newPasswordEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                newPassword = s.toString();
                LogUtils.i(newPassword);
            }
        });
        //重复新密码
        newPasswordAgainEt.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                newPasswordAgain = s.toString();
            }
        });
    }

    @Override
    protected int getLayout() {
        return R.layout.modify_password_dialog;
    }

}
