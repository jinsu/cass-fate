

PATH="$PATH:~/bin/"

# set bell off
#/usr/X11/bin/xset b off

# ---------------------------- ls, colors
alias ls='ls -GF'  # or add 'F' if you want slash
export CLICOLOR=1
export LSCOLORS=dxcxgxfxBxegedabagFxDx

# ---------------------------- cp, preserve structure
alias cp='cp -a'

# ---------------------------- pwd, respect physical
alias pwd='pwd -P'
alias grep='grep --color'
set -P


# not used anymore
# export CVSROOT=/Users/haryadi/local/1/2009b-HDFS/CVS-src

# ---------------------------- ant
export ANT_OPTS="-Xms1024m -Xmx1024m"

# ---------------------------- svn 
export SVN_EDITOR=emacs

# ---------------------------- stasis/jol/bfs/
#export STASIS_DIR=/Users/haryadi/boom/src/complete-bfs-default/stasis
#export JOL_DIR=/Users/haryadi/boom/src/complete-bfs-default/jol
#export BFS_DIR=/Users/haryadi/boom/src/complete-bfs-default/bfs
export JAVA_DIR=/System/Library/Frameworks/JavaVM.framework/Home
export JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
