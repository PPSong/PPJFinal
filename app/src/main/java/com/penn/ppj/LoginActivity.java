package com.penn.ppj;

import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.penn.ppj.databinding.ActivityLoginBinding;
import com.penn.ppj.util.PPPagerAdapter;

public class LoginActivity extends AppCompatActivity {
    //变量
    private ActivityLoginBinding binding;

    //构造函数

    //本类覆盖函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        setup();
    }

    //接口覆盖函数

    //帮助函数
    private void setup() {
        PPPagerAdapter adapter = new PPPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new LoginFragment(), getString(R.string.login));
        adapter.addFragment(new RegisterFragment(), getString(R.string.register));
        binding.viewPager.setAdapter(adapter);

        binding.mainTabLayout.setupWithViewPager(binding.viewPager);
    }
}
