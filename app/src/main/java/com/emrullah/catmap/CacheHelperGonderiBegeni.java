package com.emrullah.catmap;

import java.util.HashSet;
import java.util.Set;

public class CacheHelperGonderiBegeni {
        private static CacheHelperGonderiBegeni instance;
        private Set<String> begendiklerim = new HashSet<>();

        private CacheHelperGonderiBegeni() {}

        public static CacheHelperGonderiBegeni getInstance() {
            if (instance == null) {
                instance = new CacheHelperGonderiBegeni();
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
