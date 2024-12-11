package com.project.two.business;

import companydata.*;
import java.util.*;

public class BusinessLayer {
    private DataLayer dl;
    private final String RIT_USERNAME = "njw1389";

    public BusinessLayer() {
        dl = null;
    }

    private void openConnection() throws Exception {
        if (dl == null) {
            dl = new DataLayer(RIT_USERNAME);
        }
    }

    private void closeConnection() {
        if (dl != null) {
            dl.close();
            dl = null;
        }
    }

    // Validation methods
    private void validateCompany(String company) throws Exception {
        if (company == null || company.trim().isEmpty()) {
            throw new Exception("Company name is required");
        }
        if (!company.equals(RIT_USERNAME)) {
            throw new Exception("Company name must be your RIT username");
        }
    }

    private void validateDepartment(Department dept) throws Exception {
        if (dept.getDeptNo() == null || dept.getDeptNo().trim().isEmpty()) {
            throw new Exception("Department number is required");
        }
        if (dept.getDeptName() == null || dept.getDeptName().trim().isEmpty()) {
            throw new Exception("Department name is required");
        }
        if (dept.getLocation() == null || dept.getLocation().trim().isEmpty()) {
            throw new Exception("Location is required");
        }
        
        // Validate unique dept_no among all companies
        try {
            openConnection();
            Department existing = dl.getDepartmentNo(dept.getCompany(), dept.getDeptNo());
            if (existing != null && existing.getId() != dept.getId()) {
                throw new Exception("Department number must be unique across all companies");
            }
        } finally {
            closeConnection();
        }
    }

    private void validateEmployee(Employee emp, boolean isNew) throws Exception {
        // Remove the ID validation since it's an auto-generated integer
        // Instead validate emp_no which is the employee number string
        if (emp.getEmpNo() == null || emp.getEmpNo().trim().isEmpty()) {
            throw new Exception("Employee number is required");
        }
        if (emp.getEmpName() == null || emp.getEmpName().trim().isEmpty()) {
            throw new Exception("Employee name is required");
        }
        if (emp.getHireDate() == null) {
            throw new Exception("Hire date is required");
        }
        if (!isValidWeekday(emp.getHireDate())) {
            throw new Exception("Hire date must be a weekday (Monday-Friday)");
        }
        if (emp.getSalary() <= 0) {
            throw new Exception("Salary must be greater than zero");
        }
    
        // Validate hire date is not in future
        if (emp.getHireDate().after(new Date())) {
            throw new Exception("Hire date cannot be in the future");
        }
    
        try {
            openConnection();
            // Validate department exists
            List<Department> departments = dl.getAllDepartment(RIT_USERNAME);
            boolean deptExists = departments.stream()
                .anyMatch(d -> d.getId() == emp.getDeptId());
            if (!deptExists) {
                throw new Exception("Department does not exist");
            }
    
            // Validate manager exists (if specified)
            if (emp.getMngId() != 0) {
                Employee manager = dl.getEmployee(emp.getMngId());
                if (manager == null) {
                    throw new Exception("Manager does not exist");
                }
            }
    
            // Validate unique emp_no
            if (isNew) {
                List<Employee> allEmployees = dl.getAllEmployee(RIT_USERNAME);
                boolean empNoExists = allEmployees.stream()
                    .anyMatch(e -> e.getEmpNo().equals(emp.getEmpNo()));
                if (empNoExists) {
                    throw new Exception("Employee number must be unique");
                }
            }
        } finally {
            closeConnection();
        }
    }

    private void validateTimecard(Timecard timecard) throws Exception {
        if (timecard.getStartTime() == null || timecard.getEndTime() == null) {
            throw new Exception("Start time and end time are required");
        }
        
        Calendar start = Calendar.getInstance();
        start.setTime(timecard.getStartTime());
        Calendar end = Calendar.getInstance();
        end.setTime(timecard.getEndTime());

        // Check if times are on same day
        if (!isSameDay(start, end)) {
            throw new Exception("Start time and end time must be on the same day");
        }

        // Check if times are weekdays
        if (!isValidWeekday(timecard.getStartTime()) || !isValidWeekday(timecard.getEndTime())) {
            throw new Exception("Timecards can only be submitted for weekdays");
        }

        // Check business hours (8:00 - 18:00)
        if (!isValidTimeRange(timecard.getStartTime()) || !isValidTimeRange(timecard.getEndTime())) {
            throw new Exception("Times must be between 08:00 and 18:00");
        }

        // Check minimum duration (1 hour)
        long duration = timecard.getEndTime().getTime() - timecard.getStartTime().getTime();
        if (duration < 3600000) { // 1 hour in milliseconds
            throw new Exception("Timecard duration must be at least 1 hour");
        }

        // Check end time is after start time
        if (timecard.getEndTime().before(timecard.getStartTime())) {
            throw new Exception("End time must be after start time");
        }

        try {
            openConnection();
            Employee emp = dl.getEmployee(timecard.getEmpId());
            if (emp == null) {
                throw new Exception("Employee does not exist");
            }

            // Check for existing timecard on same day
            List<Timecard> existingCards = dl.getAllTimecard(timecard.getEmpId());
            for (Timecard existing : existingCards) {
                Calendar existingStart = Calendar.getInstance();
                existingStart.setTime(existing.getStartTime());
                if (existing.getId() != timecard.getId() && isSameDay(start, existingStart)) {
                    throw new Exception("Employee already has a timecard for this day");
                }
            }
        } finally {
            closeConnection();
        }
    }

    private boolean isValidWeekday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek >= Calendar.MONDAY && dayOfWeek <= Calendar.FRIDAY;
    }

    private boolean isValidTimeRange(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 8 && hour <= 18;
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
               cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    // Company operations
    public int deleteCompany(String companyName) throws Exception {
        try {
            validateCompany(companyName);
            openConnection();
            return dl.deleteCompany(companyName);
        } finally {
            closeConnection();
        }
    }

    // Department operations
    public Department getDepartment(String companyName, int deptId) throws Exception {
        try {
            validateCompany(companyName);
            openConnection();
            return dl.getDepartment(companyName, deptId);
        } finally {
            closeConnection();
        }
    }

    public List<Department> getAllDepartments(String companyName) throws Exception {
        try {
            validateCompany(companyName);
            openConnection();
            return dl.getAllDepartment(companyName);
        } finally {
            closeConnection();
        }
    }

    public Department insertDepartment(Department department) throws Exception {
        try {
            validateCompany(department.getCompany());
            validateDepartment(department);
            openConnection();
            return dl.insertDepartment(department);
        } finally {
            closeConnection();
        }
    }

    public Department updateDepartment(Department department) throws Exception {
        try {
            validateCompany(department.getCompany());
            validateDepartment(department);
            openConnection();
            
            Department existing = dl.getDepartment(department.getCompany(), department.getId());
            if (existing == null) {
                throw new Exception("Department not found");
            }
            
            return dl.updateDepartment(department);
        } finally {
            closeConnection();
        }
    }

    public int deleteDepartment(String company, int deptId) throws Exception {
        try {
            validateCompany(company);
            openConnection();
            
            Department existing = dl.getDepartment(company, deptId);
            if (existing == null) {
                throw new Exception("Department not found");
            }
            
            return dl.deleteDepartment(company, deptId);
        } finally {
            closeConnection();
        }
    }

    // Employee operations
    public Employee getEmployee(int empId) throws Exception {
        try {
            openConnection();
            return dl.getEmployee(empId);
        } finally {
            closeConnection();
        }
    }

    public List<Employee> getAllEmployees(String companyName) throws Exception {
        try {
            validateCompany(companyName);
            openConnection();
            return dl.getAllEmployee(companyName);
        } finally {
            closeConnection();
        }
    }

    public Employee insertEmployee(Employee employee) throws Exception {
        try {
            validateEmployee(employee, true);
            openConnection();
            return dl.insertEmployee(employee);
        } finally {
            closeConnection();
        }
    }

    public Employee updateEmployee(Employee employee) throws Exception {
        try {
            validateEmployee(employee, false);
            openConnection();
            
            Employee existing = dl.getEmployee(employee.getId());
            if (existing == null) {
                throw new Exception("Employee not found");
            }
            
            return dl.updateEmployee(employee);
        } finally {
            closeConnection();
        }
    }

    public int deleteEmployee(int empId) throws Exception {
        try {
            openConnection();
            Employee existing = dl.getEmployee(empId);
            if (existing == null) {
                throw new Exception("Employee not found");
            }
            return dl.deleteEmployee(empId);
        } finally {
            closeConnection();
        }
    }

    // Timecard operations
    public Timecard getTimecard(int timecardId) throws Exception {
        try {
            openConnection();
            return dl.getTimecard(timecardId);
        } finally {
            closeConnection();
        }
    }

    public List<Timecard> getAllTimecards(int empId) throws Exception {
        try {
            openConnection();
            return dl.getAllTimecard(empId);
        } finally {
            closeConnection();
        }
    }

    public Timecard insertTimecard(Timecard timecard) throws Exception {
        try {
            validateTimecard(timecard);
            openConnection();
            return dl.insertTimecard(timecard);
        } finally {
            closeConnection();
        }
    }

    public Timecard updateTimecard(Timecard timecard) throws Exception {
        try {
            validateTimecard(timecard);
            openConnection();
            return dl.updateTimecard(timecard);
        } finally {
            closeConnection();
        }
    }

    public int deleteTimecard(int timecardId) throws Exception {
        try {
            openConnection();
            Timecard existing = dl.getTimecard(timecardId);
            if (existing == null) {
                throw new Exception("Timecard not found");
            }
            return dl.deleteTimecard(timecardId);
        } finally {
            closeConnection();
        }
    }
}