package com.example.Abdallansasra.model;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Stud {

    @SerializedName("$id")
    private String id; // معرف المستند من Appwrite
    
    private String name;
    private Date age;

    private String imageUrl ;

    public Stud() {}

    public Stud(String name, Date age,String imageUrl   ) {
        this.name = name;
        this.age = age;
        this.imageUrl = imageUrl;

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getAge() {
        return age;
    }

    public void setAge(Date age) {
        this.age = age;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public String toString() {
        return  " "+ this.name+" "+ this.age+" "+ this.imageUrl;
    }
}
