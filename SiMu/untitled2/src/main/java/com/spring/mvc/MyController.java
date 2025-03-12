package com.spring.mvc;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

@Controller
public class MyController {

    @RequestMapping("/")
    public String showFirstView(){
        return "first-view";
    }

    @RequestMapping("/askDeteails")
    public String askEmployeeDeteils(Model model){
        model.addAttribute("employee", new Employ());
        return "ask-deteails";
    }

    @RequestMapping("/showDetails")
    public String showEmployeeDeteils(@ModelAttribute("employee") Employ emp){

        return "show-deteil";
    }



}
