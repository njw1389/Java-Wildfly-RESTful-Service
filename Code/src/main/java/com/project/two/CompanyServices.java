package com.project.two;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import companydata.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.project.two.business.BusinessLayer;

@Path("CompanyServices")
public class CompanyServices {
    private final BusinessLayer bl;
    private final Gson gson;
    
    public CompanyServices() {
        bl = new BusinessLayer();
        gson = new GsonBuilder()
        .registerTypeAdapter(java.sql.Date.class, new TypeAdapter<java.sql.Date>() {
            @Override
            public void write(JsonWriter out, java.sql.Date value) throws IOException {
                out.value(value != null ? new SimpleDateFormat("yyyy-MM-dd").format(value) : null);
            }

            @Override
            public java.sql.Date read(JsonReader in) throws IOException {
                try {
                    String dateStr = in.nextString();
                    return java.sql.Date.valueOf(dateStr);
                } catch (Exception e) {
                    throw new IOException("Failed to parse date", e);
                }
            }
        })
        .registerTypeAdapter(Timestamp.class, new TypeAdapter<Timestamp>() {
            @Override
            public void write(JsonWriter out, Timestamp value) throws IOException {
                out.value(value != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value) : null);
            }

            @Override
            public Timestamp read(JsonReader in) throws IOException {
                try {
                    String timestampStr = in.nextString();
                    return Timestamp.valueOf(timestampStr);
                } catch (Exception e) {
                    throw new IOException("Failed to parse timestamp", e);
                }
            }
        })
        .create();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "got it";
    }

    // Company Operations
    @DELETE
    @Path("/company")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteCompany(@QueryParam("company") String companyName) {
        try {
            bl.deleteCompany(companyName);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("success", companyName + "'s information deleted.");
            return responseJson.toString();
        } catch (Exception e) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("error", e.getMessage());
            return errorJson.toString();
        }
    }

    // Department Operations
    @GET
    @Path("/department")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDepartment(
            @QueryParam("company") String companyName,
            @QueryParam("dept_id") int deptId) {
        try {
            Department dept = bl.getDepartment(companyName, deptId);
            if (dept != null) {
                return gson.toJson(dept);
            } else {
                return createErrorResponse("Department not found");
            }
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @GET
    @Path("/departments")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllDepartments(@QueryParam("company") String companyName) {
        try {
            List<Department> departments = bl.getAllDepartments(companyName);
            return gson.toJson(departments);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @PUT
    @Path("/department")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateDepartment(String jsonDepartment) {
        try {
            Department dept = gson.fromJson(jsonDepartment, Department.class);
            Department updated = bl.updateDepartment(dept);
            JsonObject successJson = new JsonObject();
            successJson.add("success", gson.toJsonTree(updated));
            return successJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @POST
    @Path("/department")
    @Produces(MediaType.APPLICATION_JSON)
    public String createDepartment(
            @FormParam("company") String company,
            @FormParam("dept_name") String deptName,
            @FormParam("dept_no") String deptNo,
            @FormParam("location") String location) {
        try {
            Department dept = new Department(company, deptName, deptNo, location);
            Department created = bl.insertDepartment(dept);
            JsonObject successJson = new JsonObject();
            successJson.add("success", gson.toJsonTree(created));
            return successJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @DELETE
    @Path("/department")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteDepartment(
            @QueryParam("company") String company,
            @QueryParam("dept_id") int deptId) {
        try {
            bl.deleteDepartment(company, deptId);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("success", "Department " + deptId + " from " + company + " deleted.");
            return responseJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    // Employee Operations
    @GET
    @Path("/employee")
    @Produces(MediaType.APPLICATION_JSON)
    public String getEmployee(
            @QueryParam("company") String company,
            @QueryParam("emp_id") int empId) {
        try {
            Employee emp = bl.getEmployee(empId);
            if (emp != null) {
                return gson.toJson(emp);
            } else {
                return createErrorResponse("Employee not found");
            }
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @GET
    @Path("/employees")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAllEmployees(@QueryParam("company") String company) {
        try {
            List<Employee> employees = bl.getAllEmployees(company);
            return gson.toJson(employees);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @POST
    @Path("/employee")
    @Produces(MediaType.APPLICATION_JSON)
    public String createEmployee(
            @FormParam("company") String company,
            @FormParam("emp_name") String emp_name,
            @FormParam("emp_no") String emp_no,
            @FormParam("hire_date") String hireDateStr,
            @FormParam("job") String job,
            @FormParam("salary") double salary,
            @FormParam("dept_id") int dept_id,
            @FormParam("mng_id") int mng_id) {
        try {
            Date utilDate = new SimpleDateFormat("yyyy-MM-dd").parse(hireDateStr);
            java.sql.Date hire_date = new java.sql.Date(utilDate.getTime());
            Employee emp = new Employee(
                emp_name,
                emp_no,
                hire_date,
                job,
                salary,
                dept_id,
                mng_id
            );

            Employee created = bl.insertEmployee(emp);
            JsonObject successJson = new JsonObject();
            successJson.add("success", gson.toJsonTree(created));
            return successJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @PUT
    @Path("/employee")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateEmployee(String jsonEmployee) {
        try {
            Employee emp = gson.fromJson(jsonEmployee, Employee.class);
            Employee updated = bl.updateEmployee(emp);
            JsonObject successJson = new JsonObject();
            successJson.add("success", gson.toJsonTree(updated));
            return successJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @DELETE
    @Path("/employee")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteEmployee(
            @QueryParam("company") String company,
            @QueryParam("emp_id") int empId) {
        try {
            bl.deleteEmployee(empId);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("success", "Employee " + empId + " deleted.");
            return responseJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    // Timecard Operations
    @GET
    @Path("/timecard")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTimecard(
            @QueryParam("company") String company,
            @QueryParam("timecard_id") int timecardId) {
        try {
            Timecard timecard = bl.getTimecard(timecardId);
            if (timecard != null) {
                JsonObject responseJson = new JsonObject();
                responseJson.add("timecard", gson.toJsonTree(timecard));
                return responseJson.toString();
            } else {
                return createErrorResponse("Timecard not found");
            }
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @GET
    @Path("/timecards")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTimecards(
            @QueryParam("company") String company,
            @QueryParam("emp_id") int empId) {
        try {
            List<Timecard> timecards = bl.getAllTimecards(empId);
            return gson.toJson(timecards);
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @POST
    @Path("/timecard")
    @Produces(MediaType.APPLICATION_JSON)
    public String createTimecard(
            @FormParam("company") String company,
            @FormParam("emp_id") int empId,
            @FormParam("start_time") String startTimeStr,
            @FormParam("end_time") String endTimeStr) {
        try {
            Timestamp startTime = Timestamp.valueOf(startTimeStr);
            Timestamp endTime = Timestamp.valueOf(endTimeStr);
            Timecard timecard = new Timecard(startTime, endTime, empId);
            Timecard created = bl.insertTimecard(timecard);
            JsonObject successJson = new JsonObject();
            successJson.add("success", gson.toJsonTree(created));
            return successJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @PUT
    @Path("/timecard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String updateTimecard(String jsonTimecard) {
        try {
            Timecard timecard = gson.fromJson(jsonTimecard, Timecard.class);
            Timecard updated = bl.updateTimecard(timecard);
            JsonObject successJson = new JsonObject();
            successJson.add("success", gson.toJsonTree(updated));
            return successJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    @DELETE
    @Path("/timecard")
    @Produces(MediaType.APPLICATION_JSON)
    public String deleteTimecard(
            @QueryParam("company") String company,
            @QueryParam("timecard_id") int timecardId) {
        try {
            bl.deleteTimecard(timecardId);
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("success", "Timecard " + timecardId + " deleted.");
            return responseJson.toString();
        } catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    private String createErrorResponse(String message) {
        JsonObject response = new JsonObject();
        response.addProperty("error", message);
        return response.toString();
    }
}