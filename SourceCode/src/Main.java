import java.io.*;
import java.sql.Array;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.google.gson.Gson;


import org.json.*;
public class Main {
    
    public static ArrayList<Course> course_objects = new ArrayList<Course>();
    public static ArrayList<ElectiveCourse> nte = new ArrayList<ElectiveCourse>();
    public static ArrayList<ElectiveCourse> te = new ArrayList<ElectiveCourse>();
    public static ArrayList<ElectiveCourse> fte = new ArrayList<ElectiveCourse>();
    public static ArrayList<ElectiveCourse> ue = new ArrayList<ElectiveCourse>();

    public static Semester[] semesters = {new Semester(1),new Semester(2),new Semester(3),new Semester(4),
            new Semester(5),new Semester(6),new Semester(7),new Semester(8)};

    public static String global_semester = "";
    public static int global_max_failed_course_per_semester = 0;
    public static int global_max_student_per_semester = 0;

    public static void main(String[] args) throws JSONException {
        long startTime = System.currentTimeMillis();
        createCourseObjects();
        createElectiveCourseObjects();
        readInputJson();

        int semester = 0;
        if (global_semester.toUpperCase().equals("FALL")){
            semester = 1;
        }
        else if (global_semester.toUpperCase().equals("SPRING")){
            semester = 2;
        }
        else {
            System.out.println("Invalid semester");
            System.exit(-1);
        }

        GenerateStudent gs = new GenerateStudent(global_max_student_per_semester, semester, global_max_failed_course_per_semester);
        ArrayList<Student> students = gs.getStudents();
        

        Registration.register(students,semester);
        

        try{
            OutputGenerator.writeStudents(students);
        }
        catch (Exception e){
            System.out.println("Exception occurred. Attention here please");
            System.out.println(e);
        }


    long endTime = System.currentTimeMillis();
        System.out.print("Time elapsed: ");
        System.out.print(endTime - startTime);
        System.out.println(" ms");
    }

    public static void readInputJson(){
        String jsonString = readFile("jsonFiles/input.json");
        try {
            JSONObject obj = new JSONObject(jsonString);
            global_semester = obj.getString("semester");
            global_max_failed_course_per_semester = obj.getInt("number_of_max_failed_course_per_semester");
            global_max_student_per_semester = obj.getInt("number_of_students_per_semester");
        }
        catch (Exception e){

        }
    }
    public static void createCourseObjects(){
        String jsonString = readFile("jsonFiles/courses.json");
        try {
            JSONObject obj = new JSONObject(jsonString);
            JSONArray courses = obj.getJSONArray("courses");
            for (int i = 0; i < courses.length(); i++) {
                String courseCode = courses.getJSONObject(i).getString("course_code");
                String courseTitle = courses.getJSONObject(i).getString("course_title");
                int semesterID = courses.getJSONObject(i).getJSONObject("semester").getInt("number");
                int courseCredit = courses.getJSONObject(i).getInt("course_credit");
                JSONArray prerequisites = courses.getJSONObject(i).getJSONArray("prerequisite"); // String array
                int quota = courses.getJSONObject(i).getInt("quota");
                String type = courses.getJSONObject(i).getString("type");
                String section = courses.getJSONObject(i).getString("section");
                int n_of_students = courses.getJSONObject(i).getInt("n_of_students");


                ArrayList<Student> students = new ArrayList<Student>();
                ArrayList<String> prereq = new ArrayList<String>();
                for (int j = 0; j < prerequisites.length(); j++){
                    prereq.add(prerequisites.getString(j));
                }

                int[][] timeline = new int[9][5];
                JSONArray outer = courses.getJSONObject(i).getJSONObject("course_schedule").getJSONArray("timeline");
                for (int j = 0 ; j < outer.length(); j++){
                    JSONArray inner = courses.getJSONObject(i).getJSONObject("course_schedule").getJSONArray("timeline").getJSONArray(j);
                    for (int k = 0; k < inner.length(); k++) {
                        timeline[j][k] = inner.getInt(k);
                    }
                }
                Course created_course = new Course(courseCode, courseTitle, semesters[semesterID-1], new Schedule(timeline), students, courseCredit,
                        prereq, section, quota, n_of_students,type);

                course_objects.add(created_course);

            }

        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public static void createElectiveCourseObjects(){
        String jsonString = readFile("jsonFiles/elective.json");
        try {
            JSONObject obj = new JSONObject(jsonString);
            JSONArray courses = obj.getJSONArray("courses");//.getJSONObject(0).getString("course_code");
            for (int i = 0; i < courses.length(); i++) {
                String courseCode = courses.getJSONObject(i).getString("course_code");
                String courseTitle = courses.getJSONObject(i).getString("course_title");
                int courseCredit = courses.getJSONObject(i).getInt("course_credit");
                JSONArray prerequisites = courses.getJSONObject(i).getJSONArray("prerequisite"); // String array
                int quota = courses.getJSONObject(i).getInt("quota");
                String type = courses.getJSONObject(i).getString("type");
                String section = courses.getJSONObject(i).getString("section");
                int n_of_students = courses.getJSONObject(i).getInt("n_of_students");


                ArrayList<Student> students = new ArrayList<Student>();
                ArrayList<String> prereq = new ArrayList<String>();
                for (int j = 0; j < prerequisites.length(); j++){
                    prereq.add(prerequisites.getString(j));
                }

                int[][] timeline = new int[9][5];

                JSONArray outer = courses.getJSONObject(i).getJSONObject("course_schedule").getJSONArray("timeline");
                for (int j = 0 ; j < outer.length(); j++){
                    JSONArray inner = courses.getJSONObject(i).getJSONObject("course_schedule").getJSONArray("timeline").getJSONArray(j);
                    for (int k = 0; k < inner.length(); k++) {
                        timeline[j][k] = inner.getInt(k);
                    }
                }

                ElectiveCourse created_course = new ElectiveCourse(courseCode, courseTitle, null, new Schedule(timeline), students, courseCredit,
                        prereq, section, quota, n_of_students,type, courseTitle, courseCode);

                if (type.toUpperCase().equals("NTE")){
                    nte.add(created_course);
                }
                else if (type.toUpperCase().equals("TE")){
                    te.add(created_course);
                }
                else if (type.toUpperCase().equals("UE")){
                    ue.add(created_course);
                }
                else if (type.toUpperCase().equals("FTE")){
                    fte.add(created_course);
                }
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public static String readFile(String filename){
        File file = new File(filename);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String ret = "";
            String st;
            while ((st = br.readLine()) != null)
                ret += st;
            return ret;
        }
        catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }



}
