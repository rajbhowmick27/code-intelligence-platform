
package com.example.rebuild.swap;

import org.springframework.stereotype.Service;

@Service
public class AliasManager {

    public void swapVectorAlias(String oldAlias, String newAlias) {
        System.out.println("Swapped vector alias from " + oldAlias + " to " + newAlias);
    }

    public void swapGraphAlias(String oldAlias, String newAlias) {
        System.out.println("Swapped graph alias from " + oldAlias + " to " + newAlias);
    }
}
