package com.smart.controller;

import java.util.List;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.helper.Message;
import com.smart.model.User;

@Controller
public class HomeController {

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@RequestMapping("/")
	public String home(Model model) {
		model.addAttribute("title","Home - Smart Contact Manager");
		return "index";
	}
	
	
	@RequestMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title","Register - Smart Contact Manager");
		model.addAttribute("user",new User());
		return "signup";
	}
	
	
	@PostMapping("/do_register")
	public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result,@RequestParam(value = "agreement",defaultValue = "false") boolean agreement, Model model, HttpSession session){

		try {
			System.out.println(agreement);
			if(agreement==false) {
				System.out.println("You have not agreed the terms and conditions");
				throw new Exception("You have not agreed the terms and conditions");
			}
			
			if (result.hasErrors()) {
				model.addAttribute("user",user);
				return "signup";
			}
			
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			
			userRepository.save(user);			
			
			model.addAttribute("user", new User());			
			session.setAttribute("message", new Message("Successfully Registered","alert-success"));
			return "signup";

		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user",user);
			session.setAttribute("message", new Message("Something Went wrong !!"+e.getMessage(),"alert-danger"));
			return "signup";
		}
	}
	
	// handler for custom login
	@RequestMapping("/signin")
	public String customLogin(Model model) {
		model.addAttribute("title","Login - Smart Contact Manager");
		return "login";
	}
	
	// just testing.....
	@RequestMapping("/all")
	public String all(Model model) {
		List<User> findAll = userRepository.findAll();
		for (User user : findAll) {
			System.out.println(user);
		}
		model.addAttribute("all", findAll);
		return "all";
	}

}
