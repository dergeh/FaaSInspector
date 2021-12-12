FROM corral-tpc-raw:1.0.0

RUN apt-get install gettext -y
COPY experiments experiments
COPY exec.sh exec.sh

ENTRYPOINT ["sh", "exec.sh"]