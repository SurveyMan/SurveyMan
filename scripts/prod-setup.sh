# make ~/surveyman
# move params.properties to ~/surveyman

if [[ ! -d ~/surveyman ]] 
then
    mkdir ~/surveyman
fi

mv params.properties ~/surveyman/params.properties
