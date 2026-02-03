package com.example.Abdallansasra;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import Fragments.HomeFragment;
import Fragments.SettingFragment;
import Fragments.saf1Fragment;
import Fragments.saf2Fragment;
import Fragments.saf3Fragment;


public class MainActivity2 extends AppCompatActivity {
    BottomNavigationView btk ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
       btk = findViewById(R .id.btk);
       btk.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
           @Override
           public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

               if( menuItem.getItemId() == R.id.btmenu1)
               {

                   ChangeFragment(new SettingFragment());
                 //  Toast.makeText(MainActivity2.this, "", Toast.LENGTH_SHORT).show();
               }

               if( menuItem.getItemId() == R.id.btmenu2)
               {
                   ChangeFragment(new saf1Fragment());
                   // Toast.makeText(MainActivity2.this, "", Toast.LENGTH_SHORT).show();
               }

               if( menuItem.getItemId() == R.id.btmenu3)
               {
                   ChangeFragment(new saf2Fragment());
                  // Toast.makeText(MainActivity2.this, "", Toast.LENGTH_SHORT).show();
               }

               if( menuItem.getItemId() == R.id.btmenu4)
               {
                   ChangeFragment(new saf3Fragment());
                 //  Toast.makeText(MainActivity2.this, "", Toast.LENGTH_SHORT).show();
               }

               if( menuItem.getItemId() == R.id.btmenu5)
               {
                   ChangeFragment(new HomeFragment());
                  // Toast.makeText(MainActivity2.this, "", Toast.LENGTH_SHORT).show();
               }
               return false;
           }
       });


    }

     public void ChangeFragment(Fragment fragment)
     {
         FragmentManager frm = getSupportFragmentManager();
         FragmentTransaction ftm = frm.beginTransaction();
         ftm.replace(R.id.btfkl,fragment);
                 ftm.commit();
     }

}