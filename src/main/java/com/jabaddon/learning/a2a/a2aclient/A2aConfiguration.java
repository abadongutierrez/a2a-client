package com.jabaddon.learning.a2a.a2aclient;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "a2a.agent")
public class A2aConfiguration {
    
    private String url = "http://localhost:9090/a2a";
    private Card card = new Card();
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Card getCard() {
        return card;
    }
    
    public void setCard(Card card) {
        this.card = card;
    }
    
    public static class Card {
        private String path = "/a2a/.well-known/agent.json";
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
    }
}