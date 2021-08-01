# Global Makefile for RP2040 PIO emulator
#
# Copyright (C) 2021 Jürgen Reuter
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#
# For updates and more info or contacting the author, visit:
# <https://github.com/soundpaint/rp2040pio>
#
# Author's web site: www.juergen-reuter.de

ROOT_DIR=.
include defs.mak

all:
	cd $(JAVA_DIR) ; make -f Makefile.Server all
	cd $(JAVA_DIR) ; make -f Makefile.Monitor all
	cd $(JAVA_DIR) ; make -f Makefile.Observer all
	cd $(JAVA_DIR) ; make -f Makefile.Diagram all
	cd $(JAVA_DIR) ; make -f Makefile.GPIOObserver all
	cd $(JAVA_DIR) ; make -f Makefile.CodeObserver all
	cd $(JAVA_DIR) ; make -f Makefile.FifoObserver all
	cd $(JAVA_DIR) ; make -f Makefile.DocTool all

run: all
	cd $(JAVA_DIR) ; make -f Makefile.Server run

tags:
	- find $(JAVA_DIR) -name \*.java -exec etags {} \; -print

objclean:
	rm -rf $(ROOT_BUILD_DIR)
	rm -rf $(JAR_DIR)
	rm -rf $(RST_DOC_DIR)

bkpclean:
	- find $(JAVA_DIR) -name \*~ -exec /bin/rm -f {} \; -print
	rm -f *~

coreclean:
	rm -f core core.* vgcore.*
	rm -f $(JAVA_DIR)/core $(JAVA_DIR)/core.* $(JAVA_DIR)/vgcore.*

clean: objclean

distclean: objclean bkpclean coreclean

tarball: distclean
	@TGZ_DATE=`date +%Y-%m-%d_%H-%M-%S` ; \
	PROJECT_NAME=rpi2040pio ; \
	PROJECT_PATH=`basename \`pwd\`` ; \
	TGZ_PREFIX=$$PROJECT_NAME\_$$TGZ_DATE ; cd .. ; \
	tar cvf ./$$TGZ_PREFIX.tar.bz2 \
		--exclude=untracked \
		--exclude=.git \
		--transform=s/$$PROJECT_PATH/$$TGZ_PREFIX/ \
		--bzip2 $$PROJECT_PATH

#  Local Variables:
#    coding:utf-8
#    mode:Makefile
#  End:
