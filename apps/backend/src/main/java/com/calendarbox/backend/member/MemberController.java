package com.calendarbox.backend.member;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {
    private final MemberRepository memberRepository;

    public MemberController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @PostMapping
    public Member create(@RequestParam String name) {
        return memberRepository.save(new Member(name));
    }

    @GetMapping
    public List<Member> findAll() {
        return memberRepository.findAll();
    }
}
