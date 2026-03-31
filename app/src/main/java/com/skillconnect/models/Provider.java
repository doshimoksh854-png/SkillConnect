package com.skillconnect.models;

/**
 * Provider model — V3: added String stringId (Firebase UID), kept int id for adapters
 */
public class Provider {
    private int    id;
    private String stringId; // Firebase Auth UID
    private String name;
    private String specialty;
    private float  rating;
    private int    completedJobs;

    public Provider(int id, String name, String specialty, float rating) {
        this.id        = id;
        this.name      = name;
        this.specialty = specialty;
        this.rating    = rating;
    }

    public Provider(int id, String stringId, String name, String specialty, float rating) {
        this.id        = id;
        this.stringId  = stringId;
        this.name      = name;
        this.specialty = specialty;
        this.rating    = rating;
    }

    public int    getId()        { return id; }
    public void   setId(int id)  { this.id = id; }

    public String getStringId()          { return stringId; }
    public void   setStringId(String s)  { this.stringId = s; }

    public String getName()       { return name; }
    public void   setName(String n) { this.name = n; }

    public String getSpecialty()        { return specialty; }
    public void   setSpecialty(String s){ this.specialty = s; }

    public float  getRating()            { return rating; }
    public void   setRating(float r)     { this.rating = r; }

    public int    getCompletedJobs()     { return completedJobs; }
    public void   setCompletedJobs(int j){ this.completedJobs = j; }
}
