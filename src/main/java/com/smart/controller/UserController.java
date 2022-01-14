package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.helper.Message;
import com.smart.model.Contact;
import com.smart.model.User;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
//		System.out.println(userName);
		User user = userRepository.getUserByUserName(userName);
		model.addAttribute("user",user);		
	}

	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model,Principal principal) {
		model.addAttribute("title","User Dashboard");
		return "normal/user_dashboard";
	}
	
	// open add form handler
	@RequestMapping("/add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title","Add Contact");
		model.addAttribute("contact",new Contact());
		return "normal/add_contact_form";
	}
	
	// processing add-contact
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact,@RequestParam("profileImage") MultipartFile file ,Principal principal, HttpSession session) {
		try {
			String name = principal.getName();
			User user = userRepository.getUserByUserName(name);

			// processing and uploading file
			if(file.isEmpty()) {
				// if the file is empty then try our message
				System.out.println("File is Empty");
				contact.setImage("contact.jpeg");
			}else {
				// file the file to folder and update the name to contact
				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img/contact").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
				
				System.out.println("Image is uploaded");
				
			}
			
			// two important steps
			contact.setUser(user);
			user.getContacts().add(contact);
			
			userRepository.save(user);
			
			System.out.println("Added to database..");
			// success message
			session.setAttribute("message", new Message("Your Contact is Added Successfully...", "alert-success"));
			
		}catch(Exception e) {
			System.out.println("ERROR: "+e.getMessage());
			e.printStackTrace();
			// error message
			session.setAttribute("message", new Message("Something went wrong !! Try again ..", "alert-danger"));

		}
		return "normal/add_contact_form";
	}
	
	// show contacts handler
	// per page = 5 , current page = 0
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") int page,Model model, Principal principal){
		
		String name = principal.getName();
		User user = userRepository.getUserByUserName(name);
		
		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = contactRepository.findContactsByUser(user.getId(),pageable);
		
		if(contacts==null) {
			model.addAttribute("contacts","NoData");
		}
		else{
			model.addAttribute("contacts",contacts);
		}
		
		model.addAttribute("currentPage",page);
		model.addAttribute("totalPages",contacts.getTotalPages());
		
		model.addAttribute("title","Show User Contacts");
		return "normal/show_contacts";
	}

	// showing particular contact details
	@RequestMapping("{cId}/contact")
	public String showContactDetails(@PathVariable("cId") int cId, Model model, Principal principal) {
		Optional<Contact> contactOptional = contactRepository.findById(cId);
		Contact contact = contactOptional.get();
		
		String name = principal.getName();
		User user = userRepository.getUserByUserName(name);
		
		if(user.getId()==contact.getUser().getId()) {
			model.addAttribute("title",contact.getName());
			model.addAttribute("contact",contact);
		}
		return "normal/contact_detail";
	}
	
	// delete contact handler
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") int cId, Model model, Principal principal, HttpSession session) {
		
		Optional<Contact> optional = contactRepository.findById(cId);
		Contact contact = optional.get();

		String name = principal.getName();
		User user = userRepository.getUserByUserName(name);

		if(user.getId()==contact.getUser().getId()) {
			contact.setUser(null);
			contactRepository.delete(contact);
			session.setAttribute("message", new Message("Contact deleted successfully...", "alert-success"));
		}
		return "redirect:/user/show-contacts/0";
	}
	
	// open update form handler
	
	@PostMapping("/update-contact/{cId}")
	public String updateForm(@PathVariable("cId") int cId, Model model) {
				
		Contact contact = contactRepository.findById(cId).get();
		
		model.addAttribute("contact",contact);
		model.addAttribute("title","Update Contact");
		return "normal/update_form";
	}
	
	// update contact handler
	@RequestMapping(value = "/process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute("contact") Contact contact ,@RequestParam("profileImage") MultipartFile file, Model model, HttpSession session, Principal principal) {
		try {
			
			// old contact details
			Contact oldContactDetails = this.contactRepository.findById(contact.getcId()).get();
			if (!file.isEmpty()) {
				// file work....
				
				// delete old photo
				File deleteFile = new ClassPathResource("static/img/contact").getFile();
				
				File file1  = new File(deleteFile,oldContactDetails.getImage());
				file1.delete();
				
				
				// update new photo
				File saveFile = new ClassPathResource("static/img/contact").getFile();
				
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());
			}
			else {
				contact.setImage(oldContactDetails.getImage());
			}
			
			User user = userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			session.setAttribute("message",new Message("Your contact is updated...", "alert-success"));
			
		}catch (Exception e) {
		}
		return "redirect:/user/"+contact.getcId()+"/contact";
	}
	
	// your profile handler
	@GetMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title","Profile Page");
		return "normal/profile";
	}
	
	// open setting handler
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}
	
	// change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		
		User user = this.userRepository.getUserByUserName(principal. getName());

		if(this.bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
			
			// change the password
			user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(user);
			session.setAttribute("message", new Message("Your password is successfully changed..","alert-success"));
		}
		else {
			// error
			session.setAttribute("message", new Message("Please enter correct old password !! ","alert-danger"));
			return "redirect:/user/settings";
		}
		
		System.out.println(oldPassword);
		System.out.println(newPassword);
		return "redirect:/user/index";
	}
}
