package org.studip.unofficial_app.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import org.studip.unofficial_app.R;
import org.studip.unofficial_app.api.API;
import org.studip.unofficial_app.api.rest.StudipCourse;
import org.studip.unofficial_app.databinding.ActivityHomeBinding;
import org.studip.unofficial_app.documentsprovider.DocumentsDB;
import org.studip.unofficial_app.documentsprovider.DocumentsDBProvider;
import org.studip.unofficial_app.model.APIProvider;
import org.studip.unofficial_app.model.DBProvider;
import org.studip.unofficial_app.model.NotificationWorker;
import org.studip.unofficial_app.model.Notifications;
import org.studip.unofficial_app.model.Settings;
import org.studip.unofficial_app.model.SettingsProvider;
import org.studip.unofficial_app.model.room.DB;
import org.studip.unofficial_app.model.viewmodels.FileViewModel;
import org.studip.unofficial_app.model.viewmodels.HomeActivityViewModel;
import org.studip.unofficial_app.model.viewmodels.MessagesViewModel;
import org.studip.unofficial_app.ui.fragments.CoursesFragment;
import org.studip.unofficial_app.ui.fragments.FileFragment;
import org.studip.unofficial_app.ui.fragments.HomeFragment;
import org.studip.unofficial_app.ui.fragments.MessageFragment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomeActivity extends AppCompatActivity implements ComponentCallbacks2
{
    public static final Pattern courseFilesPattern = Pattern.compile("/dispatch\\.php/course/files?(/index)\\?cid=(\\p{Alnum}+)$");
    public static final Pattern courseFilesPatternFolder = Pattern.compile("/dispatch\\.php/course/files/index/(\\p{Alnum}+)\\?cid=(\\p{Alnum}+)$");
    
    
    public static final Pattern courseForumPattern = Pattern.compile("/plugins\\.php/coreforum/index\\?cid=(\\p{Alnum}+)$");
    
    
    public static final Pattern courseMembersPattern = Pattern.compile("/dispatch\\.php/course/members\\?cid=(\\p{Alnum}+)$");

    public static final Pattern courseCoursewarePattern = Pattern.compile("/plugins\\.php/courseware/courseware\\?cid=(\\p{Alnum}+)?(&selected=(\\p{Alnum}+))$");
    
    public static final Pattern messagePattern = Pattern.compile("/dispatch.php/messages/read/(\\p{Alnum}+)");
    
    
    
    
    private ActivityHomeBinding binding;
    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        
        binding = ActivityHomeBinding.inflate(getLayoutInflater());

        Notifications.initChannels(this);

        Settings s = SettingsProvider.getSettings(this);
        AppCompatDelegate.setDefaultNightMode(SettingsProvider.getSettings(this).theme);
        if (s.logout) {
            s.logout = false;
            //System.out.println("clearing database");
            s.safe(SettingsProvider.getSettingsPreferences(this));
            DB db = DBProvider.getDB(this);
            db.getTransactionExecutor().execute(db::clearAllTables);
            DocumentsDB docdb = DocumentsDBProvider.getDB(this);
            docdb.getTransactionExecutor().execute(docdb::clearAllTables);
            if (s.notification_service_enabled) {
                NotificationWorker.enqueue(this);
            }
            Intent intent = new Intent(this,ServerSelectActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        Activity a = this;
        
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                if (binding.pager.getAdapter() != null && tab.getPosition() < binding.pager.getAdapter().getItemCount()) {
                    binding.pager.setCurrentItem(tab.getPosition());
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab)
            {
                if (binding.tabs.getSelectedTabPosition() == binding.tabs.getTabCount()-1) {
                    tab.select();
                    Intent i = new Intent(a,SettingsActivity.class);
                    startActivity(i);
                }
                if (binding.tabs.getSelectedTabPosition() == binding.tabs.getTabCount()-2) {
                    tab.select();
                    Intent i = new Intent(a,WebViewActivity.class);
                    startActivity(i);
                }
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        
        HomeFragmentsAdapter ad = new HomeFragmentsAdapter(this);
        
        binding.pager.setAdapter(ad);
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback()
        {
            @Override
            public void onPageSelected(int position)
            {
                super.onPageSelected(position);
                binding.tabs.selectTab(binding.tabs.getTabAt(position));
            }
        });


        handleIntent();
        
        
        
        
        setContentView(binding.getRoot());
    }
    
    private void handleIntent() {
        //System.out.println("handle intent");
        Uri data = getIntent().getData();
    
        if (data != null && ! ("http".equals(data.getScheme()) || "https".equals(data.getScheme()))) {
            finishAndRemoveTask();
            return;
        }
    
        API api = APIProvider.getAPI(this);
        if (api == null)
        {
            if (data != null) {
                Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Intent intent = new Intent(this,ServerSelectActivity.class);
            startActivity(intent);
            finish();
            return;
        } else {
            if (data != null && ! api.getHostname().equals(data.getHost())) {
                finishAndRemoveTask();
                return;
            }
        }
        if (data != null) {
            // TODO handle the studip links
            boolean handled = false;
            String query = data.getQuery();
            String path = data.getPath();
            if (path == null) {
                finish();
                return;
            }
            if (query != null) {
                path  += "?"+query;
            }
            if (path.equals("/dispatch.php/start")) {
                binding.pager.setCurrentItem(0);
                handled = true;
            }
            if (path.equals("/dispatch.php/my_courses")) {
                binding.pager.setCurrentItem(1);
                handled = true;
            }
            if (path.equals("/dispatch.php/files")) {
                binding.pager.setCurrentItem(2);
                handled = true;
            }
            if (path.equals("/dispatch.php/messages/overview")) {
                binding.pager.setCurrentItem(3);
                handled = true;
            }
            //System.out.println(path);
            Matcher matcher = courseFilesPattern.matcher(path);
            if (matcher.matches()) {
                HomeActivityViewModel m = new ViewModelProvider(this).get(HomeActivityViewModel.class);
                FileViewModel f = new ViewModelProvider(this).get(FileViewModel.class);
                binding.pager.setCurrentItem(2);
                LiveData<StudipCourse> l = DBProvider.getDB(this).courseDao().observe(matcher.group(2));
                l.observe(this,(c) -> {
                    l.removeObservers(this);
                    if (c != null) {
                        m.setFilesCourse(c);
                        f.refresh(this);
                    }
                });
                handled = true;
            }
            matcher = courseFilesPatternFolder.matcher(path);
            if (matcher.matches()) {
                HomeActivityViewModel m = new ViewModelProvider(this).get(HomeActivityViewModel.class);
                FileViewModel f = new ViewModelProvider(this).get(FileViewModel.class);
                binding.pager.setCurrentItem(2);
                LiveData<StudipCourse> l = DBProvider.getDB(this).courseDao().observe(matcher.group(2));
                Matcher finalMatcher = matcher;
                l.observe(this,(c) -> {
                    l.removeObservers(this);
                    if (c != null) {
                        m.setFilesCourse(c);
                        f.setFolder(this, finalMatcher.group(1),false);
                        f.refresh(this);
                    }
                });
                handled = true;
            }
            matcher = messagePattern.matcher(path);
            if (matcher.matches()) {
                new ViewModelProvider(this).get(MessagesViewModel.class).mes.refresh(this);
                // TODO open the message
                binding.pager.setCurrentItem(3);
                handled = true;
            }
            
            
            
        
            if (path.equals("/dispatch.php/settings/general")) {
                Intent i = new Intent(this,SettingsActivity.class);
                startActivity(i);
                handled = true;
            }
        
        
            if (! handled && "https".equals(data.getScheme())) {
                Intent i = new Intent(this, WebViewActivity.class);
                i.setAction(Intent.ACTION_VIEW);
                i.setData(data);
                startActivity(i);
            }
        }
    }
    
    public static void onStatusReturn(FragmentActivity a, int status) {
        HomeActivityViewModel homem = new ViewModelProvider(a).get(HomeActivityViewModel.class);
        if (homem.connectionLostDialogShown.getValue() != null && ! homem.connectionLostDialogShown.getValue()) {
            if (status != -1)
            {
                //System.out.println(status);
                if (status == 401)
                {
                    homem.connectionLostDialogShown.setValue(true);
                    HomeActivity.showConnectionLostDialog(a, true);
                }
                else
                {
                    if (status == 403 || status == 404 || status == 405) {
                        //System.out.println("Route not enabled");
                        return;
                    }
                    if (status != 200)
                    {
                        homem.connectionLostDialogShown.setValue(true);
                        HomeActivity.showConnectionLostDialog(a, false);
                    }
                }
            }
        }
    }
    
    public static void showConnectionLostDialog(FragmentActivity a, boolean autologout) {
        Bundle b = new Bundle();
        b.putBoolean("autologout",autologout);
        ConnectionLostDialogFragment d = new ConnectionLostDialogFragment();
        d.setArguments(b);
        d.show(a.getSupportFragmentManager(),"con_lost");
    }
    
    public static class ConnectionLostDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState)
        {
            Bundle b = getArguments();
            boolean autologout = (b != null) && b.getBoolean("autologout", false);
            AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
            builder.setMessage( (autologout) ? R.string.autologout_msg : R.string.con_lost_msg);
            builder.setTitle(R.string.con_lost_title);
            if (autologout) {
                builder.setPositiveButton(R.string.autologout_relogin, (dialog, which) ->
                {
                    Intent i = new Intent(requireActivity(),LoginActivity.class);
                    startActivity(i);
                    dismiss();
                }).setNegativeButton(R.string.continue_msg,(a,c) -> dismiss());
            } else {
                builder.setNegativeButton(R.string.continue_msg,(a,c) -> dismiss());
            }
            return builder.create();
        }
    }
    
    public void navigateTo(int position) {
        binding.pager.setCurrentItem(position);
    }
    
    private static class HomeFragmentsAdapter extends FragmentStateAdapter {

        public HomeFragmentsAdapter(@NonNull FragmentActivity fragmentActivity)
        {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position)
        {
            switch (position) {
                case 3:
                    return new MessageFragment();
                case 2:
                    return new FileFragment();
                case 1:
                    return new CoursesFragment();
                case 0:
                default:
                    return new HomeFragment();
            }
        }

        @Override
        public int getItemCount()
        {
            return 4;
        }
    }
    
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN:

                
            
                break;
        
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:

                /*
                   Release any memory that your app doesn't need to run.

                   The device is running low on memory while the app is running.
                   The event raised indicates the severity of the memory-related event.
                   If the event is TRIM_MEMORY_RUNNING_CRITICAL, then the system will
                   begin killing background processes.
                */
            
                break;
        
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:

                /*
                   Release as much memory as the process can.

                   The app is on the LRU list and the system is running low on memory.
                   The event raised indicates where the app sits within the LRU list.
                   If the event is TRIM_MEMORY_COMPLETE, the process will be one of
                   the first to be terminated.
                */
            
                break;
        
            default:
                /*
                  Release any non-critical data structures.

                  The app received an unrecognized memory level value
                  from the system. Treat this as a generic low-memory message.
                */
                break;
        }
        //System.out.println("memory trim, run GC");
        System.gc();
    }
    
    
}
