package com.iteam.examproject.ui.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.navigation.NavigationBarView;
import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityMainBinding;
import com.iteam.examproject.databinding.FragmentLoginAndRegisterBinding;
import com.iteam.examproject.databinding.FragmentNoteBinding;
import com.iteam.examproject.ui.activity.MainActivity;


public class NoteFragment extends Fragment {
    private FragmentNoteBinding binding;

    public NoteFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding=FragmentNoteBinding.inflate(getLayoutInflater());
        //底部导航栏
        bottomNav();


        return binding.getRoot();
    }

    private void bottomNav() {
        //显示底部导航栏
        ActivityMainBinding mainBinding=MainActivity.activityMainBinding;
        mainBinding.bottomNavigation.setVisibility(View.VISIBLE);
        //跳转到对应Fragment
        mainBinding.bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int viewId=item.getItemId();
                Fragment fragment=null;
                if (viewId==R.id.note){
                    fragment=new NoteFragment();
                } else if (viewId == R.id.group) {
                    fragment=new GroupFragment();
                }else if(viewId==R.id.mine){
                    fragment=new MineFragment();
                }
                if (fragment!=null) {
                    getParentFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container,fragment)
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .commit();
                }
                return true;
            }
        });
    }
}