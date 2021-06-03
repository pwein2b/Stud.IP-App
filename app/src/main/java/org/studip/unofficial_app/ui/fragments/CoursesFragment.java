package org.studip.unofficial_app.ui.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;
import org.studip.unofficial_app.R;
import org.studip.unofficial_app.api.API;
import org.studip.unofficial_app.api.Features;
import org.studip.unofficial_app.api.rest.StudipCourse;
import org.studip.unofficial_app.api.rest.StudipSemester;
import org.studip.unofficial_app.databinding.CoursesEntryBinding;
import org.studip.unofficial_app.databinding.FragmentCoursesBinding;
import org.studip.unofficial_app.model.APIProvider;
import org.studip.unofficial_app.model.DBProvider;
import org.studip.unofficial_app.model.room.DB;
import org.studip.unofficial_app.model.viewmodels.CoursesViewModel;
import org.studip.unofficial_app.model.viewmodels.HomeActivityViewModel;
import org.studip.unofficial_app.ui.HomeActivity;
import org.studip.unofficial_app.ui.fragments.dialog.CourseForumDialogFragment;
import org.studip.unofficial_app.ui.fragments.dialog.CourseNewsDialogFragment;
import org.studip.unofficial_app.ui.plugins.fragments.dialog.CourseOpencastDialog;
import org.studip.unofficial_app.ui.plugins.fragments.dialog.CoursewareDialog;
import org.studip.unofficial_app.ui.plugins.fragments.dialog.MeetingsRoomsDialog;

import java.io.Serializable;
import java.util.Arrays;

public class CoursesFragment extends SwipeRefreshFragment
{
    private CoursesViewModel m;
    private HomeActivityViewModel h;
    private FragmentCoursesBinding binding;
    
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("selected_semester", (Serializable) binding.semesterSelect.getSelectedItem());
        
    }
    
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        binding = FragmentCoursesBinding.inflate(inflater);
        
        setSwipeRefreshLayout(binding.coursesRefresh);
        
        API api = APIProvider.getAPI(requireActivity());
        if (api != null && api.isFeatureEnabled(Features.FEATURE_COURSES)) {
            m = new ViewModelProvider(requireActivity()).get(CoursesViewModel.class);
            h = new ViewModelProvider(requireActivity()).get(HomeActivityViewModel.class);
            
            SemesterAdapter semad = new SemesterAdapter(requireActivity(), R.layout.list_textview);
            binding.semesterSelect.setAdapter(semad);
    
            CoursesAdapter coursead = new CoursesAdapter(requireActivity(), ArrayAdapter.NO_SELECTION);
            binding.coursesList.setAdapter(coursead);
    
            final String[] semid = {null};
    
            binding.coursesRefresh.setOnRefreshListener(() -> {
                m.semester.refresh(requireActivity());
                m.courses.refresh(requireActivity());
            });
            m.courses.isRefreshing().observe(getViewLifecycleOwner(), (ref) -> binding.coursesRefresh.setRefreshing(ref));
    
    
            m.courses.getStatus().observe(getViewLifecycleOwner(), status -> HomeActivity.onStatusReturn(requireActivity(), status));
            m.semester.getStatus().observe(getViewLifecycleOwner(), status -> HomeActivity.onStatusReturn(requireActivity(), status));
    
    
            m.semester.get().observe(getViewLifecycleOwner(), (sems) -> {
                if (sems.length == 0 && m.semester.getStatus().getValue() == -1) {
                    m.semester.refresh(requireActivity());
                }
                Arrays.sort(sems, (o1, o2) -> -Long.compare(o1.begin, o2.begin));
                StudipSemester selected = (StudipSemester) binding.semesterSelect.getSelectedItem();
                if (selected == null && savedInstanceState != null && savedInstanceState.getSerializable("selected_semester") != null) {
                    semid[0] = ((StudipSemester) savedInstanceState.getSerializable("selected_semester")).id;
                }
                semad.clear();
                semad.addAll(sems);
                long unixtime = System.currentTimeMillis() / 1000;
                for (int i = 0; i < sems.length; i++) {
                    StudipSemester s = sems[i];
                    if (semid[0] == null) {
                        if (unixtime >= s.begin && unixtime <= s.end) {
                            //System.out.println("no semester selected");
                            binding.semesterSelect.setSelection(i);
                            break;
                        }
                    }
                    else {
                        if (s.id.equals(semid[0])) {
                            //System.out.println("current semester selected");
                            binding.semesterSelect.setSelection(i);
                            break;
                        }
                    }
                }
            });
    
            DB db = DBProvider.getDB(requireActivity());
    
            final LiveData<StudipCourse[]>[] dbsem = new LiveData[]{null};
            final LiveData<StudipCourse[]> courses_saved = db.courseDao().observeAll();
            courses_saved.observe(getViewLifecycleOwner(), (c) -> {
                if (c.length == 0) {
                    //System.out.println("no courses");
                    m.courses.refresh(requireActivity());
                }
                courses_saved.removeObservers(getViewLifecycleOwner());
            });
    
    
            binding.semesterSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    semid[0] = semad.getItem(position).id;
                    if (dbsem[0] != null) {
                        dbsem[0].removeObservers(getViewLifecycleOwner());
                    }
                    // /api.php/semester/
                    //System.out.println("selected semester" + semad.getItem(position).title + "  " + semad.getItem(position).id);
                    dbsem[0] = db.courseDao().observeSemester(semad.getItem(position).id);
                    dbsem[0].observe(getViewLifecycleOwner(), (c) ->
                    {
                        //System.out.println("updated semester courses");
                        coursead.clear();
                        coursead.addAll(c);
                    });
                }
        
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        } else {
            binding.coursesRefresh.setOnRefreshListener(() -> binding.coursesRefresh.setRefreshing(false));
        }
        
        return binding.getRoot();
    }
    
    private static class SemesterAdapter extends ArrayAdapter<StudipSemester> {
        public SemesterAdapter(@NonNull Context context, int resource)
        {
            super(context, resource);
        }
    }
    
    
    private class CoursesAdapter extends ArrayAdapter<StudipCourse> {

        public CoursesAdapter(@NonNull Context context, int resource)
        {
            super(context, resource);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
        {
            StudipCourse c = getItem(position);
            View v = (convertView != null) ? convertView : getLayoutInflater().inflate(R.layout.courses_entry,parent,false);
            CoursesEntryBinding b = CoursesEntryBinding.bind(v);
            b.courseName.setText(c.title);
            if (c.modules_object != null)
            {
                if (c.modules_object.forum != null)
                {
                    b.courseForum.setVisibility(View.VISIBLE);
                }
                if (c.modules_object.documents != null)
                {
                    b.courseFiles.setVisibility(View.VISIBLE);
                }
            }
            API api = APIProvider.getAPI(requireActivity());
            if (api != null) {
                if (api.isFeatureEnabled(Features.FEATURE_COURSE_FILES) && api.isFeatureEnabled(Features.FEATURE_FILES)) {
                    b.courseFiles.setOnClickListener(v1 -> {
                        h.setFilesCourse(c);
                        HomeActivity a = (HomeActivity) requireActivity();
                        a.navigateTo(2);
                    });
                } else {
                    b.courseFiles.setVisibility(View.INVISIBLE);
                }
                if (api.isFeatureEnabled(Features.FEATURE_FORUM)) {
                    b.courseForum.setOnClickListener(v1 -> {
                        Bundle args = new Bundle();
                        args.putString("cid", c.course_id);
                        CourseForumDialogFragment forum = new CourseForumDialogFragment();
                        forum.setArguments(args);
                        forum.show(getParentFragmentManager(), "course_forum");
                    });
                } else {
                    b.courseForum.setVisibility(View.INVISIBLE);
                }
                if (api.isFeatureEnabled(Features.FEATURE_BLUBBER)) {
                    b.courseBlubber.setOnClickListener(v1 -> {
                        
                        
                    });
                } else {
                    b.courseBlubber.setVisibility(View.INVISIBLE);
                }
                if (api.isFeatureEnabled(Features.FEATURE_PLANNER)) {
                    b.courseSchedule.setOnClickListener(v1 -> {
                        
                        
                    });
                } else {
                    b.courseSchedule.setVisibility(View.INVISIBLE);
                }
            } else {
                b.courseFiles.setVisibility(View.INVISIBLE);
                b.courseForum.setVisibility(View.INVISIBLE);
                b.courseBlubber.setVisibility(View.INVISIBLE);
                b.courseSchedule.setVisibility(View.INVISIBLE);
            }
            b.courseNews.setOnClickListener(v1 ->
            {
                Bundle args = new Bundle();
                args.putString("cid", c.course_id);
                CourseNewsDialogFragment news = new CourseNewsDialogFragment();
                news.setArguments(args);
                news.show(getParentFragmentManager(), "course_news");
            });
            b.courseOpencast.setOnClickListener(v1 -> {
                Bundle args = new Bundle();
                args.putString("cid",c.course_id);
                CourseOpencastDialog opencast = new CourseOpencastDialog();
                opencast.setArguments(args);
                opencast.show(getParentFragmentManager(),"course_opencast");
            });
            b.courseCourseware.setOnClickListener(v1 -> {
                Bundle args = new Bundle();
                args.putString("cid",c.course_id);
                CoursewareDialog courseware = new CoursewareDialog();
                courseware.setArguments(args);
                requireActivity().getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(android.R.id.content, courseware, "dialog_courseware").addToBackStack(null).commit();
            });
            b.courseMeetings.setOnClickListener(v1 -> {
                Bundle args = new Bundle();
                args.putString("cid",c.course_id);
                MeetingsRoomsDialog opencast = new MeetingsRoomsDialog();
                opencast.setArguments(args);
                opencast.show(getParentFragmentManager(),"course_meetings");
            });
            return v;
        }
    }
    
}
