package com.qb.xrealsys.ifafu.web;

import android.util.Log;

import com.qb.xrealsys.ifafu.UserController;
import com.qb.xrealsys.ifafu.model.Course;
import com.qb.xrealsys.ifafu.model.Model;
import com.qb.xrealsys.ifafu.model.Syllabus;
import com.qb.xrealsys.ifafu.model.User;
import com.qb.xrealsys.ifafu.tool.GlobalLib;
import com.qb.xrealsys.ifafu.tool.HttpHelper;
import com.qb.xrealsys.ifafu.tool.HttpResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by sky on 11/02/2018.
 */

public class SyllabusInterface extends WebInterface {

    private static final String SyllabusPage = "xskbcx.aspx";

    public SyllabusInterface(String inHost, UserController userController) throws IOException {
        super(inHost, userController);
    }

    public Map<String, Model> GetSyllabus(String number, String name) throws IOException {
        User        user            = new User();
        Syllabus    syllabus        = new Syllabus();
        Map<String, Model> answer   = new HashMap<>();
        String      accessUrl = makeAccessUrlHead() + SyllabusPage;
        accessUrl += "?xh=" + number;
        accessUrl += "&xm=" + URLEncoder.encode(name, "gbk");
        accessUrl += "&gnmkdm=" + "N121603";

        Map<String, String> header = GetRefererHeader(number);
        HttpHelper   request  = new HttpHelper(accessUrl, "gbk");
        HttpResponse response = request.Get(header);

        if (response.getStatus() != 200) {
            return null;
        }

        String  html     = response.getResponse();
        if (!LoginedCheck(html)) {
            return GetSyllabus(number, name);
        }
        /* Get search option */
        getSearchOptions(html, syllabus, "id=\"xnd\"", "学年第");

        /* Get student information */
        Pattern patternA = Pattern.compile("学院：(.*)</span>");
        Pattern patternB = Pattern.compile("行政班：(.*)</span>");
        Matcher matcherA = patternA.matcher(html);
        Matcher matcherB = patternB.matcher(html);

        if (matcherA.find() && matcherB.find()) {
            user.setInstitute(matcherA.group(1));
            user.setClas(matcherB.group(1));
            user.setEnrollment(Integer.parseInt("20" + user.getClas().substring(0, 2)));
        } else {
            return null;
        }

        /* Get syllabus information */
        Map<String, List<Course>> mapNameToCourse = new HashMap<>();
        Pattern patternC = Pattern.compile("(<td( class=\"noprint\"){0,1} align=\"Center\"" +
                "( rowspan=\"\\d+\"){0,1}( width=\"\\d+%\"){0,1}>|<br>)((?!&nbsp;).*?)<br>" +
                "(.*?)<br>(.*?)<br>(.*?)(</td>|<br>)");
        Matcher matcherC = patternC.matcher(html);
        while (matcherC.find()) {
            Course course = new Course();
            course.setName(matcherC.group(5));
            course.setTeacher(matcherC.group(7));
            course.setAddress(matcherC.group(8));
            if (!analysisCourseTime(course, matcherC.group(6))) {
                analysisCourseTime2(course, html, matcherC.start(), matcherC.group(6));
            }

            if (mapNameToCourse.containsKey(course.getName())) {
                //  merge
                for (Course queryCourse: mapNameToCourse.get(course.getName())) {
                    if (queryCourse.getWeekDay() == course.getWeekDay()
                            && queryCourse.getAddress().equals(course.getAddress())) {
                        int repeatBegin = queryCourse.getWeekBegin() > course.getWeekBegin() ?
                                queryCourse.getWeekBegin() : course.getWeekBegin();
                        int repeatEnd   = queryCourse.getWeekEnd() > course.getWeekEnd() ?
                                course.getWeekEnd() : queryCourse.getWeekEnd();

                        if (repeatEnd > repeatBegin) {
                            if (queryCourse.getEnd() + 1 == course.getBegin()) {
                                queryCourse.setEnd(course.getEnd());
                                if (repeatBegin == course.getWeekBegin()) {
                                    course.setWeekBegin(repeatEnd + 1);
                                } else {
                                    course.setWeekEnd(repeatBegin - 1);
                                }
                            }
                        }
                    }
                }
            } else {
                mapNameToCourse.put(course.getName(), new ArrayList<Course>());
            }

            if (course.getWeekEnd() >= course.getWeekBegin()) {
                syllabus.append(course);
            }
            mapNameToCourse.get(course.getName()).add(course);
        }

        answer.put("user", user);
        answer.put("syllabus", syllabus);
        return answer;
    }

    private boolean analysisCourseTime(Course course, String timeString) throws IOException {
        Map<String, Integer> weekMap = new HashMap<String, Integer>() {{
            put(URLEncoder.encode("一", "GBK"), 1);
            put(URLEncoder.encode("二", "GBK"), 2);
            put(URLEncoder.encode("三", "GBK"), 3);
            put(URLEncoder.encode("四", "GBK"), 4);
            put(URLEncoder.encode("五", "GBK"), 5);
            put(URLEncoder.encode("六", "GBK"), 6);
            put(URLEncoder.encode("日", "GBK"), 0);
        }};

        Pattern pattern = Pattern.compile("周(.*)第((\\d+),)?(.*?)(\\d+)节\\{第(\\d+)-(\\d+)周(\\|(.*)周)?\\}");
        Matcher matcher = pattern.matcher(timeString);

        if (matcher.find()) {
            course.setTimeString(timeString);
            course.setWeekDay(weekMap.get(URLEncoder.encode(matcher.group(1), "GBK")));
            if (matcher.group(3) == null) {
                course.setBegin(Integer.parseInt(matcher.group(5)));
            } else {
                course.setBegin(Integer.parseInt(matcher.group(3)));
            }
            course.setEnd(Integer.parseInt(matcher.group(5)));
            course.setWeekBegin(Integer.parseInt(matcher.group(6)));
            course.setWeekEnd(Integer.parseInt(matcher.group(7)));
            if (matcher.group(8) != null) {
                if (GlobalLib.CompareUtfWithGbk("单", matcher.group(9))) {
                    course.setOddOrTwice(1);
                } else if (GlobalLib.CompareUtfWithGbk("双", matcher.group(9))) {
                    course.setOddOrTwice(2);
                }
            } else {
                course.setOddOrTwice(0);
            }

            return true;
        } else {
            return false;
        }
    }

    private void analysisCourseTime2(
            Course course, String html, int courseBeginIndex, String timeString) {

    }
}
