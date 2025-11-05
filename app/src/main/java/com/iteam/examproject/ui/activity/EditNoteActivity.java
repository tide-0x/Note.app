package com.iteam.examproject.ui.activity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.iteam.examproject.R;
import com.iteam.examproject.databinding.ActivityEditNoteBinding;
import com.iteam.examproject.ui.fragment.EditNoteFragment;

public class EditNoteActivity extends AppCompatActivity {
    private ActivityEditNoteBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityEditNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.edit_note_fragment_container_view,new EditNoteFragment())
                .commit();
    }
}