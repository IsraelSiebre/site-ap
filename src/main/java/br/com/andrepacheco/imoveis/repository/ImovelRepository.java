package br.com.andrepacheco.imoveis.repository;

import br.com.andrepacheco.imoveis.domain.Imovel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImovelRepository extends JpaRepository<Imovel, Long> {
}

