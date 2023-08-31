package study.querydsl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자 막고 싶으나 JPA 스펙 상 PROTECTED로 열어주기
@ToString(of = {"id", "username", "age"}) // 연관관계 없는 필드만
public class Member {

  @Id
  @GeneratedValue
  @Column(name = "member_id")
  private Long id;

  private String username;

  private int age;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team; // 연관관계의 주인

  public Member(String username) {
    this(username, 0);
  }

  public Member(String username, int age) {
    this(username, age, null);
  }

  public Member(String username, int age, Team team) {
    this.username = username;
    this.age = age;
    if (team != null) {
      changeTeam(team);
    }
  }

  public void changeTeam(Team team) { // 연관관계 편의 메서드
    this.team = team;
    team.getMembers().add(this);
  }
}
