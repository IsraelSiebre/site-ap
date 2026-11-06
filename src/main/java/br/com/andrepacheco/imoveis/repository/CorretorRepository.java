package br.com.andrepacheco.imoveis.repository;

import br.com.andrepacheco.imoveis.domain.Corretor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CorretorRepository extends JpaRepository<Corretor, Long> {
}

