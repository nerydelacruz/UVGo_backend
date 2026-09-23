package com.Backend.UVGo;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class UVGoController {
    public String saludar(){
        return "Nunca pares de aprender";
    }

}
