An implementation of the 
[Bayesian Personalized Recommendation](http://auai.org/uai2009/papers/UAI2009_0139_48141db02b9f0b02bc7158819ebfa2c7.pdf) 
algorithm - a matrix-factorization, ordinal-preference approach to collaborative filtering.

This implementation supports concurrent fitting of the actual factorization, as well
as concurrent predictions, and two methods of fitting (equal odds for each user, or
equal odds for each user-item interaction).

Built with Eclipse, but should build and test with vanilla mvn as well (or 
javac for that matter).