package com.penn.ppj;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.databinding.FragmentLoginBinding;

import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class LoginFragment extends Fragment {
    //变量
    private FragmentLoginBinding binding;

    //构造函数
    public LoginFragment() {
        // Required empty public constructor
    }

    //本类覆盖函数
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false);

        setup();

        return binding.getRoot();
    }

    //接口覆盖函数

    //帮助函数
    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();

        return fragment;
    }

    private void setup() {
        //设置密码键盘enter键
        binding.password.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    login();
                    return true;
                }
                return false;
            }
        });

        //检测登录按钮
        RxView.clicks(binding.loginButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                login();
                            }
                        }
                );
    }

    private void login() {
        //显示进度条
        PPApplication.showProgressDialog(getContext(), getString(R.string.logining), null);
        PPApplication.login(binding.username.getText().toString(), binding.password.getText().toString())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {

                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPApplication.hideProgressDialog();
                                getActivity().finish();
                                Intent intent = new Intent(getContext(), MainActivity.class);
                                startActivity(intent);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPApplication.error(throwable.toString());
                                //如有错误,登出以清理已经取得的信息
                                PPApplication.logout();
                                PPApplication.hideProgressDialog();
                            }
                        }
                );
    }
}
