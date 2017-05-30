package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
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
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxRadioGroup;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.ppj.databinding.FragmentLoginBinding;
import com.penn.ppj.databinding.FragmentRegisterBinding;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function6;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

import static com.penn.ppj.PPApplication.ppWarning;

public class RegisterFragment extends Fragment {
    //变量
    private FragmentRegisterBinding binding;
    private Observable<String> usernameInputObservable;
    private Observable<String> passwordInputObservable;
    private Observable<String> nicknameInputObservable;
    private Observable<String> sexInputObservable;
    private Observable<String> agreeInputObservable;
    private Observable<String> verifyCodeInputObservable;

    //构造函数
    public RegisterFragment() {
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

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false);

        setup();

        return binding.getRoot();
    }

    //接口覆盖函数

    //帮助函数
    public static RegisterFragment newInstance() {
        RegisterFragment fragment = new RegisterFragment();

        return fragment;
    }

    private void setup() {
        //用户名输入监控
        usernameInputObservable = RxTextView.textChanges(binding.username)
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
        passwordInputObservable = RxTextView.textChanges(binding.password)
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

        //验证码输入监控
        verifyCodeInputObservable = RxTextView.textChanges(binding.verifyCode)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPApplication.isVerifyCodeValid(charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.verifyCodeInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //获取验证码密码按钮监控
        RxView.clicks(binding.requestVerifyCodeButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                requestVerifyCode();
                            }
                        }
                );

        //昵称输入监控
        nicknameInputObservable = RxTextView.textChanges(binding.nickname)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPApplication.isNicknameValid(charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.nicknameInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //性别输入监控
        sexInputObservable = RxRadioGroup.checkedChanges(binding.sex)
                .skip(1)
                .map(
                        new Function<Integer, String>() {

                            @Override
                            public String apply(Integer integer) throws Exception {
                                String error = "";
                                if (integer < 0) {
                                    error = getString(R.string.error_field_required);
                                }
                                return error;
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.sexInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //同意守则勾选监控
        agreeInputObservable = RxCompoundButton.checkedChanges(binding.agree)
                .skip(1)
                .map(
                        new Function<Boolean, String>() {

                            @Override
                            public String apply(Boolean aBoolean) throws Exception {
                                String error = "";
                                if (!aBoolean) {
                                    error = getString(R.string.must_agree);
                                }
                                return error;
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.agreeInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //注册按钮是否可用
        Observable
                .combineLatest(
                        verifyCodeInputObservable,
                        usernameInputObservable,
                        passwordInputObservable,
                        nicknameInputObservable,
                        sexInputObservable,
                        agreeInputObservable,
                        new Function6<String, String, String, String, String, String, Boolean>() {

                            @Override
                            public Boolean apply(String s, String s2, String s3, String s4, String s5, String s6) throws Exception {
                                return TextUtils.isEmpty(s) && TextUtils.isEmpty(s2) && TextUtils.isEmpty(s3) && TextUtils.isEmpty(s4) && TextUtils.isEmpty(s5) && TextUtils.isEmpty(s6);
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
                                binding.registerButton.setEnabled(aBoolean);
                            }
                        }
                );

        //获取随机昵称按钮监控
        RxView.clicks(binding.getRandomNicknameButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                requestRandomNickname();
                            }
                        }
                );

        //注册按钮监控
        RxView.clicks(binding.registerButton)
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                register();
                            }
                        }
                );
    }

    private void requestVerifyCode() {
        binding.requestVerifyCodeButton.setEnabled(false);

        //控制获取验证码倒计时
        Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
                .takeWhile(
                        new Predicate<Long>() {
                            @Override
                            public boolean test(Long aLong) throws Exception {
                                return aLong <= PPApplication.REQUEST_VERIFY_CODE_INTERVAL;
                            }
                        }
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        if (aLong == PPApplication.REQUEST_VERIFY_CODE_INTERVAL) {
                            binding.requestVerifyCodeButton.setEnabled(true);
                            binding.requestVerifyCodeButton.setText(getString(R.string.get_random));
                        } else {
                            binding.requestVerifyCodeButton.setText("" + (PPApplication.REQUEST_VERIFY_CODE_INTERVAL - aLong));
                        }
                    }
                });


        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("phone", binding.username.getText().toString());

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.sendRegisterCheckCode", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            public void accept(String s) {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    PPApplication.error(ppWarn.msg);

                                    return;
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            public void accept(Throwable t1) {
                                PPApplication.error(t1.toString());
                            }
                        }
                );
    }

    private void requestRandomNickname() {
        PPJSONObject jBody = new PPJSONObject();

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.randomNickName", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            public void accept(String s) {
                                PPWarn ppWarn = PPApplication.ppWarning(s);
                                if (ppWarn != null) {
                                    PPApplication.error(ppWarn.msg);

                                    return;
                                }

                                String nickname = PPApplication.ppFromString(s, "data.nickname").getAsString();
                                binding.nickname.setText(nickname);
                            }
                        },
                        new Consumer<Throwable>() {
                            public void accept(Throwable t1) {
                                PPApplication.error(t1.toString());
                            }
                        }
                );
    }

    private void register() {
        //显示进度条
        PPApplication.showProgressDialog(getContext(), getString(R.string.register) + "...", null);
        final String tmpPhone = binding.username.getText().toString();
        final String tmpPassword = binding.password.getText().toString();

        PPJSONObject jBody = new PPJSONObject();

        int sex = binding.sex.getCheckedRadioButtonId() == R.id.male_radio ? 1 : 2;

        jBody
                .put("phone", tmpPhone)
                .put("pwd", tmpPassword)
                .put("gender", sex)
                .put("checkCode", binding.verifyCode.getText().toString())
                .put("nickname", binding.nickname.getText().toString());

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.register", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(
                        new Function<String, Observable<String>>() {
                            @Override
                            public Observable<String> apply(String s) throws Exception {
                                PPWarn ppWarn = PPApplication.ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                PPJSONObject jBody = new PPJSONObject();

                                jBody
                                        .put("phone", tmpPhone)
                                        .put("pwd", tmpPassword);

                                final Observable<String> apiResult = PPRetrofit.getInstance().api("user.login", jBody.getJSONObject());
                                return apiResult;
                            }
                        }
                )
                .flatMap(
                        new Function<String, ObservableSource<String>>() {

                            @Override
                            public ObservableSource<String> apply(@NonNull String s) throws Exception {
                                PPWarn ppWarn = PPApplication.ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                return PPApplication.login(tmpPhone, tmpPassword);
                            }
                        }
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                //隐藏进度条
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

                                //隐藏进度条
                                PPApplication.hideProgressDialog();
                            }
                        }
                );
    }
}
