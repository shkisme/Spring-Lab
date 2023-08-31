package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@SpringBootTest
@Transactional
public class MemberTest {

  @PersistenceContext
  EntityManager em;

  @Test
  @DisplayName("멤버와 팀은 성공적으로 생성되어야 한다.")
  public void 멤버와_팀은_성공적으로_생성되어야_한다() throws Exception {
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);
    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);
    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);

    em.flush();
    em.clear();

    List<Member> members = em.createQuery("select m from Member m", Member.class) // 순수 JPA로 확인!
        .getResultList();

    for (Member member : members) {
      System.out.println("member = " + member);
      System.out.println("member.getTeam() = " + member.getTeam());
    }
  }
}
