package kr.kakaotech.community.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class PolicyController {
    @GetMapping("/tos-policy")
    public String tos(Model model) {
        return "/ToSPolicy";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy(Model model) {
        return "/PrivacyPolicy";
    }
}
