package cn.qiditu.guet.android.project8;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        currentFragment = new SmsSendFragment();
        changeFragment(currentFragment);
    }

    private Fragment currentFragment;

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(!(currentFragment instanceof CameraUseFragment)
                    || !((CameraUseFragment)currentFragment).handleBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_send_message: {
                currentFragment = new SmsSendFragment();
                changeFragment(currentFragment);
            } break;
            case R.id.nav_receiver_message: {
                currentFragment = new SmsReceiverFragment();
                changeFragment(currentFragment);
            } break;
            case R.id.nav_call_phone: {
                currentFragment = new PhoneCallFragment();
                changeFragment(currentFragment);
            } break;
            case R.id.nav_phone_state_changed_listener: {
                currentFragment = new PhoneStateListenerFragment();
                changeFragment(currentFragment);
            } break;
            case R.id.nav_open_camera_app: {
                currentFragment = new CameraOpenFragment();
                changeFragment(currentFragment);
            } break;
            case R.id.nav_use_camera: {
                currentFragment = new CameraUseFragment();
                changeFragment(currentFragment);
            } break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private FragmentManager fragmentManager = this.getFragmentManager();
    private void changeFragment(@NonNull Fragment fragment) {
        fragmentManager.beginTransaction()
                        .replace(R.id.frame_layout, fragment)
                        .commitAllowingStateLoss();
    }

}
