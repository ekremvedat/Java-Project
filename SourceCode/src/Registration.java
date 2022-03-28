import com.rits.cloning.Cloner;

import java.awt.image.AreaAveragingScaleFilter;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class Registration {
    public static int semesterCount = 0;

    public static void register(ArrayList<Student> students, int semester){
        for (Student s: students){
            Transcript tr = s.getCurrentTranscript();
            ArrayList<Object[]> available_courses = new ArrayList<Object[]>();

            if(tr.getGpa().size()== 0){
                available_courses = getAvailableCoursesForRegistration(tr.getSemester(), 0.0, tr.getCoursesPassed(),tr.getCoursesFailed(),tr.getCoursesNotTaken());

            }
            else

            available_courses = getAvailableCoursesForRegistration(tr.getSemester(), tr.getGpa().get(tr.getGpa().size()-1), tr.getCoursesPassed(),tr.getCoursesFailed(),tr.getCoursesNotTaken());

            replaceElectives(available_courses);

            int creditsTakenForTheTerm = 0;
            for (Object[] course: (ArrayList<Object[]>) available_courses.clone()){
                Course curr_course = (Course)course[0];

                // Actions are logged here
                // CHECK PREREQUISITES
                boolean condition = Registration.checkPrerequisites(course, tr.getCoursesPassed());
                if (condition == false){
                    available_courses.remove(course);
                    boolean cond = true;
                    for (Object[] x : tr.getCoursesNotTaken()){
                        if (((Course)x[0]).getCourseCode().equals((curr_course).getCourseCode())){
                            cond = false;
                        }
                    }
                    if (cond)
                    tr.getCoursesNotTaken().add(course);

                    s.getLogs().add("The student " + s.getStudentId() + " could not take the course " + curr_course.getCourseCode() +
                            " because of a prerequisite dependency. Prerequisite: " + curr_course.getPrerequisites().get(0));
                    continue;
                }

                // check conflict
                Course conflict = Advisor.detectConflict(s, curr_course);
                if (conflict != null){
                    DepartmentStatistics.conflictCount++;
                    DepartmentStatistics.conflictArray.add(s.getStudentId().getId());
                    available_courses.remove(course);
                    s.getLogs().add("The course " + ((Course)course[0]).getCourseCode() + " could not be taken because it conflicts with " + conflict.getCourseCode() + ".");
                    continue;
                }

                // check quota
                if (curr_course.getNumOfStudents() >= curr_course.getQuota()){
                    DepartmentStatistics.quotaCount++;
                    DepartmentStatistics.quotaArray.add(s.getStudentId().getId());
                    available_courses.remove(course);
                    s.getLogs().add("The quota for " + curr_course.getCourseCode() + " is full. Could not take the course");
                    continue;
                }
                else {
                    curr_course.setNumOfStudents(curr_course.getNumOfStudents()+1);
                }

                // check if the student can take the engineering project course
                boolean canTake = Advisor.checkCompletedCreditsForGraduationProject(s,curr_course);
                if (!canTake){
                    DepartmentStatistics.engineeringProjectCount++;
                    DepartmentStatistics.engineeringProjectArray.add(s.getStudentId().getId());
                    s.getLogs().add("Cannot take graduation project class due to lack of total completed credits.");
                    continue;
                }

                // check if the student can take the elective course
                canTake = Advisor.checkCreditsForTechnicalElective(s,curr_course);
                if (!canTake){
                    DepartmentStatistics.teCount++;
                    DepartmentStatistics.teCountArray.add(s.getStudentId().getId());
                    s.getLogs().add("Cannot take the course " + curr_course.getCourseCode() + " because total number of completed credits are less than 155");
                    continue;
                }

                // add to schedule
                // add to active courses
                int[][] student_schedule = s.getWeeklySchedule().getCourseTime();
                int[][] course_schedule = curr_course.getCourseSchedule().getCourseTime();

                for (int i = 0 ; i < 9; i++){
                    for (int j = 0; j < 5; j++){
                        if (course_schedule[i][j] == 1){
                            student_schedule[i][j] = course_schedule[i][j];
                        }
                    }
                }
                s.getWeeklySchedule().setCourseTime(student_schedule);
                tr.setActiveCourses(available_courses);
            }
        }

        OutputGenerator.evaluateCourseStatistics();
    }

    public static void writeDepartmentOutput(){

    }

    public static boolean checkPrerequisites(Object[] checkCourse,
                                             ArrayList<ArrayList<Object[]>> globalPassedCourses) {
        // CHECK PREREQUISITES
        boolean condition = false;
        Course check = (Course) checkCourse[0];
        ArrayList<String> prerequisites = check.getPrerequisites();
        if (prerequisites.size() == 0) {
            condition = true;
        }
        for (int m = 0; m < prerequisites.size(); m++) {
            String prerequisite = prerequisites.get(m);
            for (int x = 0; x < globalPassedCourses.size(); x++) {
                ArrayList<Object[]> a = globalPassedCourses.get(x);
                for (int y = 0; y < a.size(); y++) {
                    String code = ((Course) a.get(y)[0]).getCourseCode();

                    if (code.equals(prerequisite)) {
                        condition = true;
                    }
                }
            }
        }
        return condition;
    }

    public static ArrayList<Object[]> getAvailableCoursesForRegistration(int semester, double gpa, ArrayList<ArrayList<Object[]>> globalPassedCourses,
                                                          ArrayList<ArrayList<Object[]>> globalFailedCourses, ArrayList<Object[]> notTakenCourses) {
        ArrayList<Object[]> availableCourses = new ArrayList<Object[]>();
        for (int i = 0; i < globalFailedCourses.size(); i++) {
            int count = 0;
            for (int y = 0; y < globalFailedCourses.get(i).size(); y++) {
                // check if a failed course is passed in next semesters => if so => don't add to available courses.
                for (int a = 0; a < globalPassedCourses.size(); a++) {
                    for (int c = 0; c < globalPassedCourses.get(a).size(); c++) {
                        if (((Course) (globalPassedCourses.get(a).get(c)[0])).getCourseCode().equals(((Course) (globalFailedCourses.get(i).get(y)[0])).getCourseCode())) {
                            count++;
                        }
                    }
                }
                //check if it is already in available courses because it may be in two semesters in global failed array => not to be duplicated.
                for (int z = 0; z < availableCourses.size(); z++) {
                    if (((Course) (globalFailedCourses.get(i).get(y)[0])).getCourseCode().equals(((Course) (availableCourses.get(z)[0])).getCourseCode())) {
                        count++;
                    }
                }
                if (count > 0)
                    continue;
                availableCourses.add(globalFailedCourses.get(i).get(y));
            }
        }

        for (Object[] course : notTakenCourses) {
            int counter = 0;
            for (int z = 0; z < availableCourses.size(); z++) {
                if (((Course) (course[0])).getCourseCode().equals(((Course) (availableCourses.get(z)[0])).getCourseCode())) {
                    counter++;
                }
            }
            if (counter > 0) {
                //System.out.println("The course is already taken.");
                continue;
            }
            availableCourses.add(course);

        }


        if (gpa < 1.8 && semester > 2) {
            semesterCount++;
            // loop to add all courses with DD DC note letter.// , make it optional after that
            for (int a = 0; a < globalPassedCourses.size(); a++) {
                for (int c = 0; c < globalPassedCourses.get(a).size(); c++) {
                    if (((String) (globalPassedCourses.get(a).get(c)[1])).equals("DD") || ((String) (globalPassedCourses.get(a).get(c)[1])).equals("DC")) {
                        availableCourses.add(globalPassedCourses.get(a).get(c));
                    }
                }
            }
            return availableCourses;
        }
        for (int r = 0; r < semesterCount; r++) {// if gpa in previous semester was < 1.8 => take courses of its direct next semester.
            semester -= 1;
        }

        for (int i = 0; i < Main.course_objects.size(); i++) {
            if (Main.course_objects.get(i).getGivenSemester().getNum() == semester) {
                Course myCourse = Main.course_objects.get(i);

                String grade = "XX";
                Object[] newObj = new Object[2];
                newObj[0] = myCourse;
                newObj[1] = grade;
                availableCourses.add(newObj);
            }

        }
        semesterCount = 0;

        for (int a = 0; a < globalPassedCourses.size(); a++) {
            for (int c = 0; c < globalPassedCourses.get(a).size(); c++) {
                if (((String) (globalPassedCourses.get(a).get(c)[1])).equals("DD") || ((String) (globalPassedCourses.get(a).get(c)[1])).equals("DC")) {
                    availableCourses.add(globalPassedCourses.get(a).get(c));
                }
            }
        }
        return availableCourses;
    }

    public static ArrayList<Object[]> getAvailableCoursesForSimulation(int semester, double gpa, ArrayList<ArrayList<Object[]>> globalPassedCourses,
                                                                       ArrayList<ArrayList<Object[]>> globalFailedCourses, ArrayList<Object[]> notTakenCourses) {
        ArrayList<Object[]> availableCourses = new ArrayList<Object[]>();
        for (int i =0 ; i < globalFailedCourses.size() ; i++){
            int count =0;
            for (int y=0 ; y < globalFailedCourses.get(i).size() ; y++){
                // check if a failed course is passed in next semesters => if so => don't add to available courses.
                for (int a=0 ; a < globalPassedCourses.size() ; a++) {
                    for (int c = 0; c < globalPassedCourses.get(a).size(); c++) {
                        if (((Course)(globalPassedCourses.get(a).get(c)[0])).getCourseCode().equals(((Course)(globalFailedCourses.get(i).get(y)[0])).getCourseCode())){
                            count++;
                        }
                    }
                }
                //check if it is already in available courses because it may be in two semesters in global failed array => not to be duplicated.
                for(int z =0 ; z < availableCourses.size() ; z++){
                    if (((Course)(globalFailedCourses.get(i).get(y)[0])).getCourseCode().equals(((Course)(availableCourses.get(z)[0])).getCourseCode())){
                        count++;
                    }
                }
                if (count > 0 )
                    continue;
                availableCourses.add(globalFailedCourses.get(i).get(y));
            }
        }

        for (Object[] course: notTakenCourses){
            int counter =0;
            for(int z =0 ; z < availableCourses.size() ; z++){
                if (((Course)(course[0])).getCourseCode().equals(((Course)(availableCourses.get(z)[0])).getCourseCode())){
                    counter++;
                }
            }
            if(counter > 0){
                //System.out.println("The course is already taken");
                continue;
            }
            availableCourses.add(course);

        }


        if(gpa < 1.8 && semester > 2){
            semesterCount++ ;
            // loop to add all courses with DD DC note letter.// , make it optional after that
            for (int a=0 ; a < globalPassedCourses.size() ; a++) {
                for (int c = 0; c < globalPassedCourses.get(a).size(); c++) {
                    if (((String)(globalPassedCourses.get(a).get(c)[1])).equals("DD") || ((String)(globalPassedCourses.get(a).get(c)[1])).equals("DC")){
                        availableCourses.add(globalPassedCourses.get(a).get(c));
                    }
                }
            }
            return availableCourses;
        }
        for(int r=0 ; r< semesterCount ; r++){// if gpa in previous semester was < 1.8 => take courses of its direct next semester.
            semester -=1;
        }

        for (int i = 0; i < Main.course_objects.size(); i++){
            if (Main.course_objects.get(i).getGivenSemester().getNum() == semester){
                Course myCourse = Main.course_objects.get(i);

                String grade = "XX";
                Object[] newObj = new Object[2];
                newObj[0] = myCourse;
                newObj[1] = grade;
                availableCourses.add(newObj);
            }

        }
        semesterCount =0;

        return availableCourses;

    }

    public static void replaceElectives(ArrayList<Object[]> active_courses){
        for(Object[] c: (ArrayList<Object[]>) active_courses.clone()){
            String first = ((Course) c[0]).getCourseCode().split(" ")[0];
            if ((first.equals("NTE"))){
                int size= Main.nte.size();
                int random = (int)(Math.random()*size);
                ElectiveCourse x = Main.nte.get(random);

                Cloner cloner = new Cloner();
                ElectiveCourse y = cloner.deepClone(x);

                y.setCourseCode(((Course) c[0]).getCourseCode());
                y.setCourseTitle(((Course) c[0]).getCourseTitle());

                active_courses.remove(c);
                Object[] add = new Object[2];
                add[0] = y;
                add[1] = c[1];
                active_courses.add(add);
            }
            else if ((first.equals("TE"))){
                int size= Main.te.size();
                int random = (int)(Math.random()*size);
                ElectiveCourse x = Main.te.get(random);

                Cloner cloner = new Cloner();
                ElectiveCourse y = cloner.deepClone(x);

                y.setCourseCode(((Course) c[0]).getCourseCode());
                y.setCourseTitle(((Course) c[0]).getCourseTitle());

                active_courses.remove(c);
                Object[] add = new Object[2];
                add[0] = y;
                add[1] = c[1];
                active_courses.add(add);
            }
            else if ((first.equals("UE"))){
                int size= Main.ue.size();
                int random = (int)(Math.random()*size);
                ElectiveCourse x = Main.ue.get(random);

                Cloner cloner = new Cloner();
                ElectiveCourse y = cloner.deepClone(x);

                y.setCourseCode(((Course) c[0]).getCourseCode());
                y.setCourseTitle(((Course) c[0]).getCourseTitle());

                active_courses.remove(c);
                Object[] add = new Object[2];
                add[0] = y;
                add[1] = c[1];
                active_courses.add(add);
            }
            else if ((first.equals("FTE"))){
                int size= Main.fte.size();
                int random = (int)(Math.random()*size);
                ElectiveCourse x = Main.fte.get(random);

                Cloner cloner = new Cloner();
                ElectiveCourse y = cloner.deepClone(x);

                y.setCourseCode(((Course) c[0]).getCourseCode());
                y.setCourseTitle(((Course) c[0]).getCourseTitle());

                active_courses.remove(c);
                Object[] add = new Object[2];
                add[0] = y;
                add[1] = c[1];
                active_courses.add(add);
            }
        }
    }

}

