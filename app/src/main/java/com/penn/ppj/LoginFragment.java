package com.penn.ppj;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.databinding.FragmentLoginBinding;
import com.penn.ppj.messageEvent.UserLoginEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function6;
import io.reactivex.schedulers.Schedulers;

import static com.jakewharton.rxbinding2.widget.RxTextView.textChanges;

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
                    if (binding.loginButton.isEnabled()) {
                        //如果登录按钮可用等同于点击登录按钮
                        login();
                    }
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

        //手机输入监控
        Observable<String> usernameInputObservable = RxTextView.textChanges(binding.username)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPApplication.isUsernameValid(charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.usernameInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //密码输入监控
        Observable<String> passwordInputObservable = RxTextView.textChanges(binding.password)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPApplication.isPasswordValid(charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.passwordInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //登录按钮是否可用
        Observable
                .combineLatest(
                        usernameInputObservable,
                        passwordInputObservable,
                        new BiFunction<String, String, Boolean>() {

                            @Override
                            public Boolean apply(String s, String s2) throws Exception {
                                return TextUtils.isEmpty(s) && TextUtils.isEmpty(s2);
                            }
                        }
                )
                .subscribeOn(AndroidSchedulers.mainThread())
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                binding.loginButton.setEnabled(aBoolean);
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
                                //隐藏进度条
                                PPApplication.hideProgressDialog();

                                //发出已login广播
                                EventBus.getDefault().post(new UserLoginEvent());

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

                                //隐藏进度条
                                PPApplication.hideProgressDialog();
                            }
                        }
                );
    }
}
