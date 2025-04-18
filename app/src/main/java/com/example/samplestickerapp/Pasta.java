package com.example.samplestickerapp;

import java.util.List;

public class Pasta {
    public String nome;
    public boolean ehAnimado;
    public List<String> arquivos;
    public int versao;

    public Pasta(String nome, boolean ehAnimado, List<String> arquivos, int versao) {
        this.nome = nome;
        this.ehAnimado = ehAnimado;
        this.arquivos = arquivos;
        this.versao = versao;
    }
}

