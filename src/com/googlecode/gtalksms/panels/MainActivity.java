package com.googlecode.gtalksms.panels;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.googlecode.gtalksms.MainService;
import com.googlecode.gtalksms.R;
import com.googlecode.gtalksms.XmppManager;
import com.googlecode.gtalksms.panels.tabs.BuddiesTabFragment;
import com.googlecode.gtalksms.panels.tabs.CommandsTabFragment;
import com.googlecode.gtalksms.panels.tabs.ConnectionTabFragment;
import com.googlecode.gtalksms.panels.tabs.HelpTabFragment;
import com.googlecode.gtalksms.tools.StringFmt;
import com.googlecode.gtalksms.tools.Tools;
import com.googlecode.gtalksms.xmpp.XmppFriend;

public class MainActivity extends SherlockFragmentActivity {
    
    class TabListener implements ActionBar.TabListener {

        private SherlockFragment fragment;

        public TabListener(SherlockFragment fragment) {
            this.fragment = fragment;
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
            ft.add(R.id.fragment_container, fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
         }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            ft.add(R.id.fragment_container, fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            ft.remove(fragment);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
    }

    private AdView mAdView;
    private MainService mMainService;
    private ActionBar mActionBar;
    private ConnectionTabFragment mConnectionTabFragment = new ConnectionTabFragment();
    private BuddiesTabFragment mBuddiesTabFragment = new BuddiesTabFragment();
    private CommandsTabFragment mCommandsTabFragment = new CommandsTabFragment();
    private HelpTabFragment mHelpTabFragment = new HelpTabFragment();
     
    private BroadcastReceiver mXmppreceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MainService.ACTION_XMPP_PRESENCE_CHANGED)) {
                int stateInt = intent.getIntExtra("state", XmppFriend.OFFLINE);
                String userId = intent.getStringExtra("userid");
                String userFullId = intent.getStringExtra("fullid");
                String name = intent.getStringExtra("name");
                String status = intent.getStringExtra("status");

                mBuddiesTabFragment.updateBuddy(userId, userFullId, name, status, stateInt);
            } else if (action.equals(MainService.ACTION_XMPP_CONNECTION_CHANGED)) {
                updateStatus(intent.getIntExtra("new_state", 0));
            }
        }
    };;
    
    private ServiceConnection _mainServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mMainService = ((MainService.LocalBinder) service).getService();
            mMainService.updateBuddies();
            updateStatus(mMainService.getConnectionStatus());
        }

        public void onServiceDisconnected(ComponentName className) {
            mMainService = null;
        }
    };

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setTitle(StringFmt.Style("GTalkSMS " + Tools.getVersionName(getBaseContext()), Typeface.BOLD));
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setSupportProgressBarIndeterminateVisibility(false);
       
        setContentView(R.layout.tab_container);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mActionBar.addTab(mActionBar.newTab().setText("Connection").setTabListener(new TabListener(mConnectionTabFragment)));
        mActionBar.addTab(mActionBar.newTab().setText("About").setTabListener(new TabListener(mHelpTabFragment)));
       
        if (Tools.isDonateAppInstalled(getBaseContext())) {
            findViewById(R.id.StatusBar).setVisibility(View.GONE);
        } else {
            mAdView = new AdView(this, AdSize.BANNER, "a14e5a583244738");
            mAdView.loadAd(new AdRequest());
            mAdView.setBackgroundColor(Color.TRANSPARENT);
            
            LinearLayout mainLayout = (LinearLayout) findViewById(R.id.MainLayout);
            mainLayout.addView(mAdView, 1);

            TextView marketLink = (TextView) findViewById(R.id.MarketLink);
            marketLink.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Tools.openLink(MainActivity.this, "market://details?id=com.googlecode.gtalksmsdonate");
                }
            });
            
            TextView donateLink = (TextView) findViewById(R.id.DonateLink);
            donateLink.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    Tools.openLink(MainActivity.this, "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=WQDV6S67WAC7A&lc=US&item_name=GTalkSMS&item_number=WEB&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted");
                }
            });
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        unbindService(_mainServiceConnection);
        unregisterReceiver(mXmppreceiver);
    }
    
    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(MainService.ACTION_XMPP_PRESENCE_CHANGED);
        intentFilter.addAction(MainService.ACTION_XMPP_CONNECTION_CHANGED);
        registerReceiver(mXmppreceiver, intentFilter);
        Intent intent = new Intent(MainService.ACTION_CONNECT);
        bindService(intent, _mainServiceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Settings").setIcon(R.drawable.ic_menu_preferences).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("Settings")) {
            Intent intent = new Intent(MainActivity.this, Preferences.class);
            intent.putExtra("panel", R.xml.prefs_all);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent i = new Intent(this, LogCollector.class);
            startActivity(i);
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }
    
    private void updateStatus(int status) {
        mConnectionTabFragment.updateStatus(status);
        setSupportProgressBarIndeterminateVisibility(false);
        switch (status) {
            case XmppManager.CONNECTED:
                mActionBar.setIcon(R.drawable.icon_green);
                break;
            case XmppManager.DISCONNECTED:
                mActionBar.setIcon(R.drawable.icon_red);
                break;
            case XmppManager.CONNECTING:
            case XmppManager.DISCONNECTING:
                setSupportProgressBarIndeterminateVisibility(true);
                mActionBar.setIcon(R.drawable.icon_orange);
                break;
            case XmppManager.WAITING_TO_CONNECT:
            case XmppManager.WAITING_FOR_NETWORK:
                mActionBar.setIcon(R.drawable.icon_blue);
                break;
            default:
                throw new IllegalStateException();
        }
        
        if (status == XmppManager.CONNECTED) {
            mCommandsTabFragment.updateCommands(mMainService.getCommandSet());
            mActionBar.addTab(mActionBar.newTab().setText("Buddies").setTabListener(new TabListener(mBuddiesTabFragment)), 1);
            mActionBar.addTab(mActionBar.newTab().setText("Commands").setTabListener(new TabListener(mCommandsTabFragment)), 2);
        } else {
            if (removeTab("Buddies") || removeTab("Commands")) {
                mActionBar.setSelectedNavigationItem(0);
            }
        }
    }
    
    private boolean removeTab(String name) {
        boolean result = false;
        for (int i = 0 ; i < mActionBar.getTabCount() ; ++i) {
            if (mActionBar.getTabAt(i).getText().equals(name)) {
                if (mActionBar.getSelectedNavigationIndex() == i) {
                    result = true;
                }
                
                mActionBar.removeTabAt(i);
                i--;
            }
        }
        return result;
    }
}
