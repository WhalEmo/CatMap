package com.beem.catmap.data.local;

import java.util.HashSet;
import java.util.Set;

public class CacheHelperPostLike {
        private static CacheHelperPostLike instance;
        private Set<String> begendiklerim = new HashSet<>();

        private CacheHelperPostLike() {}

        public static CacheHelperPostLike getInstance() {
            if (instance == null) {
                instance = new CacheHelperPostLike();
            }
            return instance;
        }

        public void setBegeniList(Set<String> gonderiIdList) {
            this.begendiklerim = gonderiIdList;
        }

        public boolean begenmisMi(String gonderiId) {
            return begendiklerim.contains(gonderiId);
        }

        public void begen(String gonderiId) {
            begendiklerim.add(gonderiId);
        }

        public void begeniKaldir(String gonderiId) {
            begendiklerim.remove(gonderiId);
        }

}
