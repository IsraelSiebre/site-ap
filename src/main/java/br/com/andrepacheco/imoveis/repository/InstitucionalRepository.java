package br.com.andrepacheco.imoveis.repository;

import br.com.andrepacheco.imoveis.domain.Institucional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitucionalRepository extends JpaRepository<Institucional, Long> {
}

