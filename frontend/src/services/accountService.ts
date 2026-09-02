import { apiClient } from './apiClient'
import type { Address, AddressRequest, Profile, UpdateProfileRequest } from '../types/commerce'

export const accountService = {
  async getProfile(): Promise<Profile> { return (await apiClient.get<Profile>('/account/profile')).data },
  async updateProfile(request: UpdateProfileRequest): Promise<Profile> { return (await apiClient.put<Profile>('/account/profile', request)).data },
  async listAddresses(): Promise<Address[]> { return (await apiClient.get<Address[]>('/account/addresses')).data },
  async createAddress(request: AddressRequest): Promise<Address> { return (await apiClient.post<Address>('/account/addresses', request)).data },
  async updateAddress(id: number, request: AddressRequest): Promise<Address> { return (await apiClient.put<Address>(`/account/addresses/${id}`, request)).data },
  async deleteAddress(id: number): Promise<void> { await apiClient.delete(`/account/addresses/${id}`) },
}
