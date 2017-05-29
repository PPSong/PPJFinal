package com.penn.ppj;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.penn.ppj.databinding.ActivityMainBinding;
import com.penn.ppj.util.PPPagerAdapter;

import static com.penn.ppj.R.id.fab;

public class MainActivity extends AppCompatActivity {
    //变量
    private ActivityMainBinding binding;

    //构造函数

    //本类覆盖函数
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        setup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //接口覆盖函数

    //帮助函数
    private void setup() {
        //toolbar
        setSupportActionBar(binding.toolbar);
        //设置toolbar高度和padding
        AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) binding.toolbar.getLayoutParams();
        layoutParams.height = PPApplication.getStatusBarAddActionBarHeight();
        binding.toolbar.setLayoutParams(layoutParams);
        binding.toolbar.setPadding(0, PPApplication.getStatusBarHeight(), 0, 0);

        //view pager
        PPPagerAdapter adapter = new PPPagerAdapter(getSupportFragmentManager());
        NearbyFragment nearbyFragment = new NearbyFragment();
        adapter.addFragment(nearbyFragment, "C1");

        binding.viewPager.setAdapter(adapter);

        //有几个tab就设几防止page自己重新刷新
        binding.viewPager.setOffscreenPageLimit(3);

        binding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }
}
