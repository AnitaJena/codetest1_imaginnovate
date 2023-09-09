package com.imaginnovate.CodingTest.Controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.imaginnovate.CodingTest.Entity.Employee;
import com.imaginnovate.CodingTest.pojo.EmployeeRequestPojo;
import com.imaginnovate.CodingTest.pojo.EmployeeResponsePojo;
import com.imaginnovate.CodingTest.repository.EmployeeRepository;

@RestController
public class CodingTestController {
	
	@Autowired
	EmployeeRepository employeeRepository;
	
	@PostMapping("/saveEmployee")
    ResponseEntity<?> addStudent(@Valid @RequestBody EmployeeRequestPojo employeeRequestPojo,BindingResult bindingResult) {
		 Map<String, String> maps = new HashMap<>();
		 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		 System.out.println(employeeRequestPojo.toString());
		 Employee employee=new Employee();
		if (bindingResult.hasErrors()) { //it will check the validation of pojo class 
			bindingResult.getAllErrors().forEach((error) -> {
			        String fieldName = ((FieldError) error).getField();
			        String errorMessage = error.getDefaultMessage();
			        maps.put(fieldName, errorMessage);
			    });
            return  ResponseEntity.ok(maps);
		}   		    
		//parse the Doj
		employee.setDoj(LocalDate.parse(employeeRequestPojo.getDoj(),formatter));
		
		BeanUtils.copyProperties(employeeRequestPojo,employee);// here copy from the pojo class property to entity class
		employee=employeeRepository.save(employee); // save the entity in database
		
		if(employee !=null) {
		maps.put("success", "Save Successfully");
		}
        return ResponseEntity.ok(maps);
    }

	@GetMapping(value = "/getEmployeelist")
	public ResponseEntity<?> getEmployeeList() throws Exception {
		List<EmployeeResponsePojo> employeeList = new ArrayList<>();
		List<Employee> list = employeeRepository.findAll();
		for (Employee e : list) {
			EmployeeResponsePojo employeeResponsePojo = new EmployeeResponsePojo();
			employeeResponsePojo.setEmployeeCode(e.getId());
			employeeResponsePojo.setFirstName(e.getFirstName());
			employeeResponsePojo.setLastName(e.getLastName());
			LocalDate currentDate = LocalDate.now(); // get current date
			LocalDate doj = e.getDoj();

			// Calculate the number of months between DOJ and the current date
			int monthsWorked = (int) Period.between(doj, currentDate).toTotalMonths();

			System.out.println("monthsWorked" + monthsWorked);
			if (monthsWorked <= 0) {
				// Employee hasn't started working yet, no salary or tax to calculate
				employeeResponsePojo.setTaxAmount(0.0);
				employeeResponsePojo.setYearlySalary(0.0);
				employeeResponsePojo.setCessAmount(0.0);
			}

			// Calculate the monthly salary
			BigDecimal monthlySalary = e.getSalary();

			// Calculate the total salary for the financial year
			Double totalSalary = monthlySalary.doubleValue() * 12;
			// calculate total Yearly Tax
			Double totalYearlyTax = calculateTaxAmount(totalSalary);
			// calculate the total monthly tax
			Double totalMonthlyTax = totalYearlyTax / 12;
			BigDecimal totalmonthTax = new BigDecimal(totalMonthlyTax).setScale(2, RoundingMode.HALF_UP);
			System.out.println("totalMonthlyTax" + totalmonthTax);
			employeeResponsePojo.setTaxAmount(monthsWorked * totalmonthTax.doubleValue()); // calculate the total tax using DOJ and total working month
			employeeResponsePojo.setYearlySalary(totalSalary);
			employeeResponsePojo.setCessAmount(calculateCessAmount(totalSalary));
			employeeList.add(employeeResponsePojo);
		}
		return ResponseEntity.ok(employeeList);
	}
	public double calculateTaxAmount(double yearlySalary) {
	    double taxAmount = 0.0;
	    
	    if (yearlySalary <= 250000) {
	        taxAmount = 0.0;
	    } else if (yearlySalary <= 500000) {
	        taxAmount = (yearlySalary - 250000) * 0.05;
	    } else if (yearlySalary <= 1000000) {
	        taxAmount = 250000 * 0.05 + (yearlySalary - 500000) * 0.10;
	    } else {
	        taxAmount = 250000 * 0.05 + 500000 * 0.10 + (yearlySalary - 1000000) * 0.20;
	    }	    
	    return taxAmount;
	}
	public double calculateCessAmount(double yearlySalary) {
	    if (yearlySalary > 2500000) {
	        double cessAmount = (yearlySalary - 2500000) * 0.02;
	        return cessAmount;
	    }
	    return 0.0;
	}
}
