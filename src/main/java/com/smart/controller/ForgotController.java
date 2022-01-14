package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.helper.Message;
import com.smart.model.User;
import com.smart.service.EmailService;

@Controller
public class ForgotController {

	Random random = new Random(1000);

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
	// email id form open handler
	@RequestMapping("/forgot")
	public String openEmailFom() {
		return "forgot_email_form";
	}
	
	// email id form open handler
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {
		System.out.println(email);
		
		// generating otp of 4 digits
		int otp = random.nextInt(9999);
		System.out.println(otp);
		
		// write code for send otp to email
		
		String subject = "OTP From SCM";
		String message = "<h1> OTP = <b>"+otp+"</b></h1>";
		String to = email;
		
		boolean flag = this.emailService.sendEmail(subject, message, to);
		
		if(flag) {
			session.setAttribute("myotp",otp);
			session.setAttribute("email",email);
			return "verify_otp";			
		}
		else {
			session.setAttribute("message", new Message("Check your email again....", "alert-danger"));
			return "forgot_email_form";
		}
		
	}
	
	// verify otp
	
	@PostMapping("/verify-otp")
	public String verifyOTP(@RequestParam("otp") int otp, HttpSession session) {
	
		int myOtp = (int)session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");
		if(myOtp==otp) {
			// password change form
			
			User user = this.userRepository.getUserByUserName(email);
			if (user==null) {
				// send error message
				session.setAttribute("message",new Message("No User exist with this email!!", "alert-warning"));
				return "forgot_email_form";
			}
			else {
				// send change password form
				return "password_change_form";
			}
		}
		else {
			session.setAttribute("message", new Message("You have entered wrong otp" , "alert-danger"));
			return "verify_otp";
		}
		
	}
	// change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword") String newPassword, HttpSession session) {
		
		String email = (String)session.getAttribute("email");
		User user = userRepository.getUserByUserName(email);
		user.setPassword(bCryptPasswordEncoder.encode(newPassword));
		this.userRepository.save(user);
		
		return "redirect:/signin?change=Password changed successfully..";
	}
}
