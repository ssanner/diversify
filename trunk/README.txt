
Result Set Diversifier -- A Java framework for evaluating various
                          result set diversification algorithms.

Author: Scott Sanner (ssanner@gmail.com)


Description
===========

This code implements a Java framework for producing result
sets for queries on a corpus (currently given as a directory
of text files).  It is intended as a testing framework for
diversification algorithms, but may be used for general 
result list construction in information retrieval.

The code provides an implementation of Maximal Marginal Relevance 
(MMR) (Carbonell & Goldstein, SIGIR 1998) along with a variety of 
kernels:

  * Term Frequency (TF) Kernel
  
  * Term Frequency - Inverse Document Frequency (TF-IDF) Kernel
    n.b., currently uses non-log TF and log IDF, but variants
          could be used, c.f., 
          http://cseweb.ucsd.edu/~elkan/papers/spire05.pdf
  
  * BM25 Kernel
    c.f., http://research.microsoft.com/en-us/people/junxu/airs2010_rankusekernel.pdf

  * LDA Kernel
    n.b., essentially an LSI Kernel using LDA to derive topic-document distributions
    
  * Probabilistic Latent Set Relevance (PLSR) Kernel
    Preliminary work on the derivation of this kernel appeared as
     
      Probabilistic Latent Maximal Marginal Relevance, SIGIR 2010, 
      S. Guo and S. Sanner.

    Currently implemented kernel and derivation unpublished.
    

Basic Installation and Invocation
=================================

diversify/ provides the following subdirectories:

    src   All source code (.java files)
    bin   All binaries (.class files)
    lib   All 3rd party libraries (.jar files)
    files All supplementary files (i.e., data, results)

Always ensure that all .jar files in lib/ are included in your
CLASSPATH for both Java compilation and at Java runtime.  It is
recommended that you use Eclipse for Java development:

    http://www.eclipse.org/downloads/

In Eclipse the CLASSPATH libraries can be set via 

    Project -> Properties -> Java Build Path -> Libraries Tab

For running this code from a terminal, there are two scripts

    run     For Windows/Cygwin and UNIX/Linux systems
    run.bat For the Windows CMD prompt


Starting Point
==============

See class TestDiversity in the default package, which evaluates
a variety of MMR algorithms (with different kernels) w.r.t.
various queries on the news content in files/data.  The command
line parameters are as follows:

    *   arg 1: directory of files to rank
    *   arg 2: directory for output
    *   arg 3: query (enclose in 'single quotes')

The code exports results both to stdout and the output directory 
with filename constructed according to the query.

> Sample data:

Each directory in files/data contains 50 documents for news articles 
retrieved with the query given by the directory name.

> Examples:

From Eclipse, run TestDiversity with the following arguments, 
or use the following commands (substituting 'run' with 'run.bat' 
if working on a non-Cygwin Windows system).

    ./run TestDiversity files/data/Healthcare files/results 'health legislation' 
    ./run TestDiversity files/data/BP_Oil files/results 'legal charges' 
    ./run TestDiversity files/data/Barack_Obama/ files/results 'gun control lobby'

> Debugging:

Most classes (e.g., MMR and all Kernels) have a DEBUG flag, which 
if set to true will printout debug information as the code in that 
class executes.  For the LDAKernel and PLSRKernel, setting DEBUG=true 
will display the topic models for each document.

