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
	cd java ; make all

run: all
	cd java ; make run

objclean:
	rm -rf $(BUILD_DIR)
	rm -rf $(JAR_DIR)

bkpclean:
	cd java ; make bkpclean
	rm -f *~

coreclean:
	cd java ; make coreclean
	rm -f core core.* vgcore.*

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
